/*
 * Copyright (C) 2017-2021 The Technical University of Denmark
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

import java.util.Arrays;

/**
 * A sequence of states, generated from a level and client, or a log file.
 * Stores static elements of the level common to all states.
 * <p>
 * When loading a level, perform sanity checks: 1. Maximum size of level's
 * bounding rectangle must fit in a 16-bit signed integer for each dimension.
 * I.e. maximum size is 32,767 x 32,767 cells, everything inclusive. - Checked
 * during parsing. 2. All relevant areas should be enclosed by walls. - Checked
 * in this.checkObjectsEnclosedInWalls(). 3. Each enclosed area must have enough
 * boxes for all goals and at least one agent to manipulate those boxes. -
 * Checked in this.checkObjectsEnclosedInWalls(). 4. Box goals that no agent can
 * reach or which boxes' no reaching agents can manipulate, must be solved in
 * the initial state. - Checked in this.checkObjectsEnclosedInWalls().
 * <p>
 * StateSequence objects are thread-safe to write only from a single thread (the
 * protocol thread), but can be read from any number threads (e.g. the GUI
 * threads). The state is up-to-date with calls to getNumStates().
 */
class StateSequence {

  /**
   * Sequence of states. states[0] is the initial state.
   * <p>
   * If allowDiscardingPastStates is true, then we will replace states[0] every
   * action, in which case it is not the initial state, but the latest state (and
   * all past states are discarded).
   * <p>
   * IMPORTANT! Note that numStates is used for memory visibility to GUI threads.
   * Take care with order of reading/writing these variables to ensure correct
   * visibility. Only the protocol thread may write these variables (after
   * construction), while any thread may read them. Reading threads is only
   * guaranteed to get an up-to-date snapshot of the variables as of that thread's
   * last read of numStates through .getNumStates().
   */
  private State[] states = new State[64];
  private long[] stateTimes = new long[64];
  private volatile int numStates;

  /**
   * Set if we are allowed to discard past states (i.e. we're not using a GUI).
   */
  private boolean allowDiscardingPastStates = false;

  LevelInfo levelInfo = null;

  StateSequence(LevelInfo levelInfo) {
    this.levelInfo = levelInfo;

    if (this.levelInfo.initialSequence == null) {
      this.states[0] = levelInfo.initialState;
      this.stateTimes[0] = 0;
      this.numStates = 1;
    } else {
      this.states = levelInfo.initialSequence.states;
      this.stateTimes = levelInfo.initialSequence.stateTimes;
      this.numStates = levelInfo.initialSequence.numStates;
    }
  }

  void allowDiscardingPastStates() {
    this.allowDiscardingPastStates = true;
  }

  /**
   * Gets the number of available states. NB! This is a volatile read, so any
   * subsequent read of this.states is "up-to-date" as of this call.
   */
  int getNumStates() {
    return this.numStates;
  }

  /**
   * Gets the time in nanoseconds for when the given state was generated.
   */
  long getStateTime(int state) {
    return this.stateTimes[state];
  }

  State getState(int state) {
    return this.states[state];
  }

  /**
   * Returns whether there is a wall at the given (row, col). Complexity: O(1).
   */
  boolean wallAt(short row, short col) {
    return this.levelInfo.wallAt(row, col);
  }

  /**
   * Binary searches for a box goal cell at the given (row, col) and returns the
   * index if found, and -1 otherwise. Complexity: O(log(numBoxGoals)).
   */
  int findBoxGoal(short row, short col) {
    return this.levelInfo.findBoxGoal(row, col);
  }

  /**
   * Does a binary search over the sorted boxes in the latest state, and returns
   * the index in the sorted order where the box with the given (row, col) should
   * be. Complexity: O(log(numBoxes)).
   */
  private int findBox(State currentState, short row, short col) {
    int lowIdx = 0;
    int highIdx = this.levelInfo.numBoxes - 1;
    int midIdx = lowIdx + (highIdx - lowIdx) / 2;

    while (lowIdx <= highIdx) {
      midIdx = lowIdx + (highIdx - lowIdx) / 2;
      short midRow = currentState.boxRows[this.levelInfo.sortedBoxIds[midIdx]];
      short midCol = currentState.boxCols[this.levelInfo.sortedBoxIds[midIdx]];

      if (midRow < row) {
        lowIdx = midIdx + 1;
      } else if (midRow > row) {
        highIdx = midIdx - 1;
      } else if (midCol < col) {
        lowIdx = midIdx + 1;
      } else if (midCol > col) {
        highIdx = midIdx - 1;
      } else {
        // Found.
        break;
      }
    }

    return midIdx;
  }

  /**
   * Search for box at the given (row, col) in the latest state. Returns box
   * letter (A..Z = 0..25) if found, and -1 otherwise. Complexity:
   * O(log(numBoxes)).
   */
  byte boxAt(short row, short col) {
    if (this.levelInfo.numBoxes == 0) {
      return -1;
    }
    State currentState = this.states[this.numStates - 1];
    int sortedBoxIdx = this.findBox(currentState, row, col);
    int boxId = this.levelInfo.sortedBoxIds[sortedBoxIdx];
    if (currentState.boxRows[boxId] == row && currentState.boxCols[boxId] == col) {
      return this.levelInfo.boxLetters[boxId];
    }
    return -1;
  }

  /**
   * Moves a box in newState from the given (fromRow, fromCol) to (toRow, toCol)
   * and maintains this.levelInfo.sortedBoxIds.
   */
  void moveBox(State newState, short fromRow, short fromCol, short toRow, short toCol) {
    int sortedBoxIdx = this.findBox(newState, fromRow, fromCol);
    int boxId = this.levelInfo.sortedBoxIds[sortedBoxIdx];

    short[] boxRows = newState.boxRows;
    short[] boxCols = newState.boxCols;

    // Shift sortedBoxIds until sorted order is restored.
    if (toRow > fromRow || (toRow == fromRow && toCol > fromCol)) {
      while (sortedBoxIdx + 1 < this.levelInfo.numBoxes
          && (boxRows[this.levelInfo.sortedBoxIds[sortedBoxIdx + 1]] < toRow
              || (boxRows[this.levelInfo.sortedBoxIds[sortedBoxIdx + 1]] == toRow
                  && boxCols[this.levelInfo.sortedBoxIds[sortedBoxIdx + 1]] < toCol))) {
        this.levelInfo.sortedBoxIds[sortedBoxIdx] = this.levelInfo.sortedBoxIds[sortedBoxIdx + 1];
        ++sortedBoxIdx;
      }
    } else {
      while (0 < sortedBoxIdx && (boxRows[this.levelInfo.sortedBoxIds[sortedBoxIdx - 1]] > toRow
          || (boxRows[this.levelInfo.sortedBoxIds[sortedBoxIdx - 1]] == toRow
              && boxCols[this.levelInfo.sortedBoxIds[sortedBoxIdx - 1]] > toCol))) {
        this.levelInfo.sortedBoxIds[sortedBoxIdx] = this.levelInfo.sortedBoxIds[sortedBoxIdx - 1];
        --sortedBoxIdx;
      }
    }

    // Move box.
    boxRows[boxId] = toRow;
    boxCols[boxId] = toCol;
    this.levelInfo.sortedBoxIds[sortedBoxIdx] = boxId;
  }

  /**
   * Search for an agent at the given (row, col) in the latest state. Returns
   * agent ID (0..9) if found, and -1 otherwise. Complexity: O(numAgents).
   */
  byte agentAt(short row, short col) {
    State currentState = this.states[this.numStates - 1];
    for (byte a = 0; a < this.levelInfo.numAgents; ++a) {
      if (currentState.agentRows[a] == row && currentState.agentCols[a] == col) {
        return a;
      }
    }
    return -1;
  }

  /**
   * Moves the given agent to the given (row, col). Complexity: O(1).
   */
  void moveAgent(State newState, byte agent, short row, short col) {
    newState.agentRows[agent] = row;
    newState.agentCols[agent] = col;
  }

  /**
   * Checks if the given cell is free (no wall, box, or agent occupies it).
   * Complexity: O(log(numBoxes) + numAgents).
   */
  boolean freeAt(short row, short col) {
    return !this.wallAt(row, col) && this.boxAt(row, col) == -1 && this.agentAt(row, col) == -1;
  }

  /**
   * Determines which actions are applicable and non-conflicting. Returns an array
   * with true for each action which was applicable and non-conflicting, and false
   * otherwise.
   */
  boolean[] isApplicable(Action[] jointAction, State currentState) {
    // TODO: could be set as some external context, when in separate class
    var numAgents = currentState.agentRows.length;

    boolean[] applicable = new boolean[numAgents];
    short[] destRows = new short[numAgents];
    short[] destCols = new short[numAgents];
    short[] boxRows = new short[numAgents];
    short[] boxCols = new short[numAgents];

    // Test applicability.
    for (byte agent = 0; agent < numAgents; ++agent) {
      Action action = jointAction[agent];
      short agentRow = currentState.agentRows[agent];
      short agentCol = currentState.agentCols[agent];
      boxRows[agent] = (short) (agentRow + action.boxDeltaRow);
      boxCols[agent] = (short) (agentCol + action.boxDeltaCol);
      byte boxLetter;

      // Test for applicability.
      switch (action.type) {
        case NoOp:
          applicable[agent] = true;
          break;

        case Move:
          destRows[agent] = (short) (agentRow + action.moveDeltaRow);
          destCols[agent] = (short) (agentCol + action.moveDeltaCol);
          applicable[agent] = this.freeAt(destRows[agent], destCols[agent]);
          break;

        case Push:
          boxLetter = this.boxAt(boxRows[agent], boxCols[agent]);
          destRows[agent] = (short) (boxRows[agent] + action.moveDeltaRow);
          destCols[agent] = (short) (boxCols[agent] + action.moveDeltaCol);
          applicable[agent] = boxLetter != -1
              && this.levelInfo.agentColors[agent] == this.levelInfo.boxColors[boxLetter]
              && this.freeAt(destRows[agent], destCols[agent]);
          break;

        case Pull:
          boxRows[agent] = (short) (agentRow - action.boxDeltaRow);
          boxCols[agent] = (short) (agentCol - action.boxDeltaCol);
          boxLetter = this.boxAt(boxRows[agent], boxCols[agent]);
          destRows[agent] = (short) (agentRow + action.moveDeltaRow);
          destCols[agent] = (short) (agentCol + action.moveDeltaCol);
          applicable[agent] = boxLetter != -1
              && this.levelInfo.agentColors[agent] == this.levelInfo.boxColors[boxLetter]
              && this.freeAt(destRows[agent], destCols[agent]);
          break;
      }
    }

    // Test conflicts.
    boolean[] conflicting = new boolean[numAgents];
    for (byte a1 = 0; a1 < numAgents; ++a1) {
      if (!applicable[a1] || jointAction[a1] == Action.NoOp) {
        continue;
      }
      for (byte a2 = 0; a2 < a1; ++a2) {
        if (!applicable[a2] || jointAction[a2] == Action.NoOp) {
          continue;
        }

        // Objects moving into same cell?
        if (destRows[a1] == destRows[a2] && destCols[a1] == destCols[a2]) {
          conflicting[a1] = true;
          conflicting[a2] = true;
        }

        // Moving same box?
        if (boxRows[a1] == boxRows[a2] && boxCols[a1] == boxCols[a2]) {
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

  /**
   * Applies the actions in jointAction which are applicable to the latest state
   * and returns the resulting state.
   */
  private State apply(Action[] jointAction, boolean[] applicable) {
    State currentState = this.states[this.numStates - 1];
    State newState = new State(currentState);

    for (byte agent = 0; agent < jointAction.length; ++agent) {
      if (!applicable[agent]) {
        // Inapplicable or conflicting action - do nothing instead.
        continue;
      }

      Action action = jointAction[agent];
      short newAgentRow;
      short newAgentCol;
      short oldBoxRow;
      short oldBoxCol;
      short newBoxRow;
      short newBoxCol;

      switch (action.type) {
        case NoOp:
          // Do nothing.
          break;

        case Move:
          newAgentRow = (short) (currentState.agentRows[agent] + action.moveDeltaRow);
          newAgentCol = (short) (currentState.agentCols[agent] + action.moveDeltaCol);
          this.moveAgent(newState, agent, newAgentRow, newAgentCol);
          break;

        case Push:
          newAgentRow = (short) (currentState.agentRows[agent] + action.boxDeltaRow);
          newAgentCol = (short) (currentState.agentCols[agent] + action.boxDeltaCol);
          oldBoxRow = newAgentRow;
          oldBoxCol = newAgentCol;
          newBoxRow = (short) (oldBoxRow + action.moveDeltaRow);
          newBoxCol = (short) (oldBoxCol + action.moveDeltaCol);
          this.moveBox(newState, oldBoxRow, oldBoxCol, newBoxRow, newBoxCol);
          this.moveAgent(newState, agent, newAgentRow, newAgentCol);
          break;

        case Pull:
          newAgentRow = (short) (currentState.agentRows[agent] + action.moveDeltaRow);
          newAgentCol = (short) (currentState.agentCols[agent] + action.moveDeltaCol);
          oldBoxRow = (short) (currentState.agentRows[agent] - action.boxDeltaRow);
          oldBoxCol = (short) (currentState.agentCols[agent] - action.boxDeltaCol);
          newBoxRow = currentState.agentRows[agent];
          newBoxCol = currentState.agentCols[agent];
          this.moveAgent(newState, agent, newAgentRow, newAgentCol);
          this.moveBox(newState, oldBoxRow, oldBoxCol, newBoxRow, newBoxCol);
          break;
      }
    }

    return newState;
  }

  /**
   * Execute a joint action. Returns a boolean array with success for each agent.
   */
  boolean[] execute(Action[] jointAction, long actionTime) {
    // Determine applicable and non-conflicting actions.
    var currentState = this.states[this.numStates - 1];
    boolean[] applicable = this.isApplicable(jointAction, currentState);

    // Create new state with applicable and non-conflicting actions.
    State newState = this.apply(jointAction, applicable);

    // Update this.states and this.numStates. Grow as necessary.
    if (this.allowDiscardingPastStates) {
      this.states[0] = newState;
      this.stateTimes[0] = actionTime;
      // NB. This change will not be visible to other threads; if we needed that we
      // could set
      // numStates = 1.
    } else {
      if (this.states.length == this.numStates) {
        this.states = Arrays.copyOf(this.states, this.states.length * 2);
        this.stateTimes = Arrays.copyOf(this.stateTimes, this.stateTimes.length * 2);
      }
      this.states[this.numStates] = newState;
      this.stateTimes[this.numStates] = actionTime;

      // Non-atomic increment OK, only the protocol thread may write to numStates.
      // NB. This causes visibility of the new state to other threads.
      // noinspection NonAtomicOperationOnVolatileField
      ++this.numStates;
    }

    return applicable;
  }
}
