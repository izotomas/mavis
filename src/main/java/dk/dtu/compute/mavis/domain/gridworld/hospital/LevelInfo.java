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

import java.awt.Color;
import java.util.BitSet;

/**
 * Contains information that does not change throughout the run.
 */
class LevelInfo {
  /**
   * Bookkeeping for writing/reading logs.
   */
  String levelName;
  String clientName;

  /**
   * Size of bounding rectangle around level.
   */
  short numRows;
  short numCols;

  /**
   * Walls. Indexed by (row * numCols + col). For a maximum size level (32767 x
   * 32767 cells) this takes ~128 MiB memory.
   */
  BitSet walls;

  /**
   * Boxes. The letters are indexed by box id (0 .. numBoxes-1). The colors are
   * indexed by the box letter (A..Z = 0..25). The locations are stored in the
   * individual states.
   */
  int numBoxes;
  byte[] boxLetters;
  Color[] boxColors;

  /**
   * Agents. The colors are indexed by agent id (0 .. 9). The locations are stored
   * in the individual states.
   */
  byte numAgents;
  Color[] agentColors;

  /**
   * Agent goal cells. Indexed by agent id (0 .. numAgents-1). Values are -1 if
   * agent has no goal cell.
   */
  short[] agentGoalRows;
  short[] agentGoalCols;

  /**
   * Box goal cells. Indexed by box goal id (0 .. numBoxGoals-1).
   * <p>
   * This collection happens to be lexicographically sorted by (row, col) due to
   * the parsing order. We can therefore do binary search if we want to check if a
   * specific (row, col) coordinate is a box goal cell.
   */
  int numBoxGoals;
  short[] boxGoalRows;
  short[] boxGoalCols;
  byte[] boxGoalLetters;

  /**
   * Ids of boxes as they would be in the latest state if the boxes were sorted by
   * (row, col). I.e. sortedBoxIds[i] < sortedBoxIds[i+1] with < defined by
   * lexicographical ordering of (row, col). The value at sortedBoxIds[i] is the
   * box id of the box at that position in the ordering in the latest state. E.g.
   * the sortedBoxIds[11] is the box id of the 12th box in the sorted order, and
   * will index into this.states[this.numStates-1].boxRows/boxCols. Note that
   * during this.apply, this array does NOT index into
   * this.states[this.numStates-1].boxRows/boxCols yet, since the new state is not
   * added to the this.states array yet.
   */
  int[] sortedBoxIds;

  /**
   * The initial state.
   */
  State initialState;

  /**
   * The initial sequence (may be null).
   */
  StateSequence initialSequence;

  public LevelInfo() {
    this.levelName = null;
    this.clientName = null;
    this.numRows = 0;
    this.numCols = 0;
    this.walls = null;

    this.numBoxes = 0;
    this.boxColors = new Color[26];

    this.numAgents = 0;
    this.agentColors = new Color[10];

    this.agentGoalRows = null;
    this.agentGoalCols = null;

    this.numBoxGoals = 0;
    this.boxGoalRows = null;
    this.boxGoalCols = null;
    this.boxGoalLetters = null;
    this.sortedBoxIds = null;
  }

  public LevelInfo(String levelName, String clientName, short numRows, short numCols, BitSet walls,
      int numBoxes, byte[] boxLetters, Color[] boxColors, byte numAgents, Color[] agentColors,
      short[] agentGoalRows, short[] agentGoalCols, int numBoxGoals, short[] boxGoalRows,
      short[] boxGoalCols, byte[] boxGoalLetters, State initialState, int[] sortedBoxIds) {
    this.levelName = levelName;
    this.clientName = clientName;
    this.numRows = numRows;
    this.numCols = numCols;
    this.walls = walls;
    this.numBoxes = numBoxes;
    this.boxLetters = boxLetters;
    this.boxColors = boxColors;
    this.numAgents = numAgents;
    this.agentColors = agentColors;
    this.agentGoalRows = agentGoalRows;
    this.agentGoalCols = agentGoalCols;
    this.numBoxGoals = numBoxGoals;
    this.boxGoalRows = boxGoalRows;
    this.boxGoalCols = boxGoalCols;
    this.boxGoalLetters = boxGoalLetters;
    this.sortedBoxIds = sortedBoxIds;
    this.initialState = initialState;
  }

  /**
   * Search for box goal at the given (row, col). Returns box goal letter (A..Z =
   * 0..25) if found, and -1 otherwise. Complexity: O(log(numBoxGoals)).
   */
  byte boxGoalAt(short row, short col) {
    int boxGoal = this.findBoxGoal(row, col);
    if (boxGoal != -1) {
      return this.boxGoalLetters[boxGoal];
    }
    return -1;
  }

  /**
   * Binary searches for a box goal cell at the given (row, col) and returns the
   * index if found, and -1 otherwise. Complexity: O(log(numBoxGoals)).
   */
  int findBoxGoal(short row, short col) {
    int lowIdx = 0;
    int highIdx = this.numBoxGoals - 1;

    while (lowIdx <= highIdx) {
      int midIdx = lowIdx + (highIdx - lowIdx) / 2;
      short midRow = this.boxGoalRows[midIdx];
      short midCol = this.boxGoalCols[midIdx];

      if (midRow < row) {
        lowIdx = midIdx + 1;
      } else if (midRow > row) {
        highIdx = midIdx - 1;
      } else if (midCol < col) {
        lowIdx = midIdx + 1;
      } else if (midCol > col) {
        highIdx = midIdx - 1;
      } else {
        return midIdx;
      }
    }

    return -1;
  }

  /**
   * Returns whether there is a wall at the given (row, col). Complexity: O(1).
   */
  boolean wallAt(short row, short col) {
    return this.walls.get(row * this.numCols + col);
  }
}
