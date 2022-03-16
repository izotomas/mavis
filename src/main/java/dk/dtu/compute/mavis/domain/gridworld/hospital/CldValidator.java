/*
 * Copyright (C) 2017-2022 The Technical University of Denmark
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package dk.dtu.compute.mavis.domain.gridworld.hospital;

import java.util.List;
import java.util.stream.Collectors;
import org.javatuples.Pair;
import dk.dtu.compute.cdl.model.Constraint;
import dk.dtu.compute.cdl.services.ConstraintBuilder;
import dk.dtu.compute.mavis.domain.gridworld.Validator;

class CldValidator implements Validator<Action, State> {

  private final LevelInfo levelInfo;
  private final List<ConstraintBuilder> singleConstraints;
  private final List<ConstraintBuilder> multiConstraints;

  public CldValidator(LevelInfo levelInfo, List<ConstraintBuilder> constraints) {
    this.levelInfo = levelInfo;

    var groups = constraints.stream()
        .collect(Collectors.partitioningBy(ConstraintBuilder::isSingleContextConstraint));
    this.singleConstraints = groups.get(true);
    this.multiConstraints = groups.get(false);
  }

  @Override
  public boolean[] isApplicable(Action[] jointAction, State state) {
    var numAgents = this.levelInfo.numAgents;
    var applicable = new boolean[numAgents];

    var actions = map(jointAction, state);
    boolean[] conflicting = new boolean[numAgents];
    for (byte a1 = 0; a1 < numAgents; ++a1) {
      var action = actions[a1];
      // Test for applicability.
      var isMoving = !action.origin.equals(action.destination);
      applicable[action.agent] =
          isMoving ? this.freeAt(action.destination, state) && isApplicable(action)
              : isApplicable(action);

      for (byte a2 = 0; a2 < a1; ++a2) {
        var other = actions[a2];
        // Test for conflicts.
        var isConflicting = isConflicting(action, other);
        if (isConflicting) {
          conflicting[a1] = true;
          conflicting[a2] = true;
        }
      }
    }

    for (byte agent = 0; agent < numAgents; ++agent) {
      applicable[agent] &= !conflicting[agent];
    }

    return applicable;
  }

  private boolean isApplicable(dk.dtu.compute.cdl.model.Action action) {
    var blocked = singleConstraints.stream().map(b -> b.withRequestingContext(action))
        .map(ConstraintBuilder::build).anyMatch(Constraint::evaluate);
    return !blocked;
  }

  private boolean isConflicting(dk.dtu.compute.cdl.model.Action action,
      dk.dtu.compute.cdl.model.Action other) {
    var conflicting = multiConstraints.stream().map(b -> b.withRequestingContext(action))
        .map(b -> b.withRestrictingContext(other)).map(ConstraintBuilder::build)
        .anyMatch(Constraint::evaluate);
    return conflicting;
  }


  /**
   * Checks if the given cell is free (no wall, box, or agent occupies it).
   * Complexity: O(log(numAgents)).
   */
  private boolean freeAt(Pair<Integer, Integer> vertex, State state) {
    var row = vertex.getValue0().shortValue();
    var col = vertex.getValue1().shortValue();
    return !this.wallAt(row, col) && this.agentAt(row, col, state) == -1;
  }

  /**
   * Returns whether there is a wall at the given (row, col). Complexity: O(1).
   */
  private boolean wallAt(short row, short col) {
    return this.levelInfo.wallAt(row, col);
  }

  /**
   * Search for an agent at the given (row, col) in the latest state. Returns
   * agent ID (0..9) if found, and -1 otherwise. Complexity: O(numAgents).
   */
  private byte agentAt(short row, short col, State state) {
    for (byte a = 0; a < this.levelInfo.numAgents; ++a) {
      if (state.agentRows[a] == row && state.agentCols[a] == col) {
        return a;
      }
    }
    return -1;
  }


  private static dk.dtu.compute.cdl.model.Action[] map(Action[] jointAction, State state) {
    var jointCldAction = new dk.dtu.compute.cdl.model.Action[jointAction.length];

    for (var agent = 0; agent < jointAction.length; agent++) {
      var action = jointAction[0];
      var origRow = Integer.valueOf(state.agentRows[agent]);
      var origCol = Integer.valueOf(state.agentCols[agent]);
      var origin = new Pair<>(origRow, origCol);

      switch (action.type) {
        case NoOp:
          jointCldAction[agent] = map(origin, origin, state.time, action.name, agent);
          break;
        case Move:
          var destRow = Integer.valueOf(origRow + action.moveDeltaRow);
          var destCol = Integer.valueOf(origCol + action.moveDeltaCol);
          var dest = new Pair<>(destRow, destCol);
          jointCldAction[agent] = map(origin, dest, state.time, action.name, agent);
          break;
        default:
          throw new IllegalArgumentException("Only Move and NoOp actions are supported.");
      }

    }
    return jointCldAction;
  }

  private static dk.dtu.compute.cdl.model.Action map(Pair<Integer, Integer> origin,
      Pair<Integer, Integer> dest, Integer time, String name, Integer agent) {
    return new dk.dtu.compute.cdl.model.Action(origin, dest, time, name, agent);
  }
}
