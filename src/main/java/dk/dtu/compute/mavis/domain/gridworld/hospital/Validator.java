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

public interface Validator {

  /**
   * Determines which actions are applicable and non-conflicting. Returns an array
   * with true for each action which was applicable and non-conflicting, and false
   * otherwise.
   */
  boolean[] isApplicable(Action[] jointAction, State state);
}


class HospitalValidator implements Validator {

  private final LevelInfo levelInfo;

  public HospitalValidator(LevelInfo levelInfo) {
    this.levelInfo = levelInfo;
  }

  /**
   * Checks if the given cell is free (no wall, box, or agent occupies it).
   * Complexity: O(log(numBoxes) + numAgents).
   */
  private boolean freeAt(short row, short col, State state) {
    return !this.wallAt(row, col) && this.boxAt(row, col, state) == -1
        && this.agentAt(row, col, state) == -1;
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

  /**
   * Search for box at the given (row, col) in the state. Returns box letter (A..Z
   * = 0..25) if found, and -1 otherwise. Complexity: O(log(numBoxes)).
   */
  private byte boxAt(short row, short col, State state) {
    if (this.levelInfo.numBoxes == 0) {
      return -1;
    }
    int sortedBoxIdx = this.findBox(row, col, state);
    int boxId = this.levelInfo.sortedBoxIds[sortedBoxIdx];
    if (state.boxRows[boxId] == row && state.boxCols[boxId] == col) {
      return this.levelInfo.boxLetters[boxId];
    }
    return -1;
  }

  /**
   * Does a binary search over the sorted boxes in the latest state, and returns
   * the index in the sorted order where the box with the given (row, col) should
   * be. Complexity: O(log(numBoxes)).
   */
  private int findBox(short row, short col, State state) {
    int lowIdx = 0;
    int highIdx = this.levelInfo.numBoxes - 1;
    int midIdx = lowIdx + (highIdx - lowIdx) / 2;

    while (lowIdx <= highIdx) {
      midIdx = lowIdx + (highIdx - lowIdx) / 2;
      short midRow = state.boxRows[this.levelInfo.sortedBoxIds[midIdx]];
      short midCol = state.boxCols[this.levelInfo.sortedBoxIds[midIdx]];

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
   * Determines which actions are applicable and non-conflicting. Returns an array
   * with true for each action which was applicable and non-conflicting, and false
   * otherwise.
   */
  public boolean[] isApplicable(Action[] jointAction, State state) {
    if (this.levelInfo == null) {
      throw new UnsupportedOperationException("Missing Level Context");
    }

    var numAgents = this.levelInfo.numAgents;

    boolean[] applicable = new boolean[numAgents];
    short[] destRows = new short[numAgents];
    short[] destCols = new short[numAgents];
    short[] boxRows = new short[numAgents];
    short[] boxCols = new short[numAgents];

    // Test applicability.
    for (byte agent = 0; agent < numAgents; ++agent) {
      Action action = jointAction[agent];
      short agentRow = state.agentRows[agent];
      short agentCol = state.agentCols[agent];
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
          applicable[agent] = this.freeAt(destRows[agent], destCols[agent], state);
          break;

        case Push:
          boxLetter = this.boxAt(boxRows[agent], boxCols[agent], state);
          destRows[agent] = (short) (boxRows[agent] + action.moveDeltaRow);
          destCols[agent] = (short) (boxCols[agent] + action.moveDeltaCol);
          applicable[agent] = boxLetter != -1
              && this.levelInfo.agentColors[agent] == this.levelInfo.boxColors[boxLetter]
              && this.freeAt(destRows[agent], destCols[agent], state);
          break;

        case Pull:
          boxRows[agent] = (short) (agentRow - action.boxDeltaRow);
          boxCols[agent] = (short) (agentCol - action.boxDeltaCol);
          boxLetter = this.boxAt(boxRows[agent], boxCols[agent], state);
          destRows[agent] = (short) (agentRow + action.moveDeltaRow);
          destCols[agent] = (short) (agentCol + action.moveDeltaCol);
          applicable[agent] = boxLetter != -1
              && this.levelInfo.agentColors[agent] == this.levelInfo.boxColors[boxLetter]
              && this.freeAt(destRows[agent], destCols[agent], state);
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
}
