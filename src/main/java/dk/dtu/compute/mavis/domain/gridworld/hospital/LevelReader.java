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

import dk.dtu.compute.mavis.domain.ParseException;
import dk.dtu.compute.mavis.domain.gridworld.Validator;
import dk.dtu.compute.mavis.server.Server;

import java.awt.Color;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Produces LevelInfo.
 */
class LevelReader {

  private final Path domainFile;
  private final boolean isLogFile;
  private LevelInfo levelInfo;
  private StateSequence stateSequence;
  private Validator<Action, State> validator;

  LevelReader(Path domainFile, boolean isLogFile) {
    this.domainFile = domainFile;
    this.isLogFile = isLogFile;
    this.validator = null;
  }

  /**
   * Parses the given level file to construct a new state sequence.
   */
  public LevelInfo getLevel() throws IOException, ParseException {
    this.levelInfo = new LevelInfo();
    var tStart = System.nanoTime();

    try (LineNumberReader levelReader =
        new LineNumberReader(Files.newBufferedReader(domainFile, StandardCharsets.US_ASCII))) {
      try {
        // Skip the domain type lines.
        levelReader.readLine();
        levelReader.readLine();

        String line = levelReader.readLine();
        if (line == null || !line.equals("#levelname")) {
          throw new ParseException("Expected beginning of level name section (#levelname).",
              levelReader.getLineNumber());
        }
        line = this.parseNameSection(levelReader);

        if (line == null || !line.equals("#colors")) {
          throw new ParseException("Expected beginning of color section (#colors).",
              levelReader.getLineNumber());
        }
        line = this.parseColorsSection(levelReader);

        if (!line.equals("#initial")) {
          throw new ParseException("Expected beginning of initial state section (#initial).",
              levelReader.getLineNumber());
        }
        line = this.parseInitialSection(levelReader);

        if (!line.equals("#goal")) {
          throw new ParseException("Expected beginning of goal state section (#goal).",
              levelReader.getLineNumber());
        }
        line = this.parseGoalSection(levelReader);

        // Initial and goal states loaded; check that states are legal.
        this.checkObjectsEnclosedInWalls();

        if (!line.stripTrailing().equalsIgnoreCase("#end")) {
          throw new ParseException("Expected end section (#end).", levelReader.getLineNumber());
        }
        line = parseEndSection(levelReader);
        this.validator = new HospitalValidator(levelInfo);

        // If this is a log file, then parse additional sections.
        if (isLogFile) {

          this.stateSequence = new StateSequence(this.levelInfo);

          // Parse client name.
          if (!line.stripTrailing().equalsIgnoreCase("#clientname")) {
            throw new ParseException("Expected client name section (#clientname).",
                levelReader.getLineNumber());
          }
          line = parseClientNameSection(levelReader);

          // Parse and simulate actions.
          if (!line.stripTrailing().equalsIgnoreCase("#actions")) {
            throw new ParseException("Expected actions section (#actions).",
                levelReader.getLineNumber());
          }
          line = parseActionsSection(levelReader);

          if (!line.stripTrailing().equalsIgnoreCase("#end")) {
            throw new ParseException("Expected end section (#end).", levelReader.getLineNumber());
          }
          line = parseEndSection(levelReader);

          // Parse summary to check if it is consistent with simulation.
          if (!line.stripTrailing().equalsIgnoreCase("#solved")) {
            throw new ParseException("Expected solved section (#solved).",
                levelReader.getLineNumber());
          }
          line = parseSolvedSection(levelReader);

          if (!line.stripTrailing().equalsIgnoreCase("#numactions")) {
            throw new ParseException("Expected numactions section (#numactions).",
                levelReader.getLineNumber());
          }
          line = parseNumActionsSection(levelReader);

          if (!line.stripTrailing().equalsIgnoreCase("#time")) {
            throw new ParseException("Expected time section (#time).", levelReader.getLineNumber());
          }
          line = parseTimeSection(levelReader);

          if (!line.stripTrailing().equalsIgnoreCase("#end")) {
            throw new ParseException("Expected end section (#end).", levelReader.getLineNumber());
          }
          line = parseEndSection(levelReader);
        }

        if (line != null) {
          throw new ParseException("Expected no more content after end section.",
              levelReader.getLineNumber());
        }

        this.levelInfo.initialSequence = this.stateSequence;
      } catch (MalformedInputException e) {
        throw new ParseException("Level file content not valid ASCII.",
            levelReader.getLineNumber());
      }
    }

    var tEnd = System.nanoTime();
    Server.printDebug(String.format("Parsing time: %.3f ms.", (tEnd - tStart) / 1_000_000_000.0));
    return this.levelInfo;
  }

  private static class LocationStack {
    private int size;
    private short[] rows;
    private short[] cols;

    LocationStack(int initialCapacity) {
      this.size = 0;
      this.rows = new short[initialCapacity];
      this.cols = new short[initialCapacity];
    }

    int size() {
      return this.size;
    }

    void clear() {
      this.size = 0;
    }

    void push(short row, short col) {
      if (this.size == this.rows.length) {
        this.rows = Arrays.copyOf(this.rows, this.rows.length * 2);
        this.cols = Arrays.copyOf(this.cols, this.cols.length * 2);
      }
      this.rows[this.size] = row;
      this.cols[this.size] = col;
      ++this.size;
    }

    short topRow() {
      return this.rows[this.size - 1];
    }

    short topCol() {
      return this.cols[this.size - 1];
    }

    void pop() {
      --this.size;
    }
  }

  /**
   * Verifies that every object (agent, agent goal, box, and box goal) are in
   * areas enclosed by walls.
   */
  private void checkObjectsEnclosedInWalls() throws ParseException {
    State initialState = this.levelInfo.initialState;
    LocationStack stack = new LocationStack(1024);
    BitSet visitedCells = new BitSet(this.levelInfo.numRows * this.levelInfo.numCols);

    for (byte a = 0; a < this.levelInfo.numAgents; ++a) {
      short agentRow = initialState.agentRows[a];
      short agentCol = initialState.agentCols[a];
      if (!this.isContainedInWalls(agentRow, agentCol, visitedCells, stack)) {
        throw new ParseException(
            String.format("Agent '%s' is not in an area enclosed by walls.", (char) (a + '0')));
      }

      short agentGoalRow = this.levelInfo.agentGoalRows[a];
      short agentGoalCol = this.levelInfo.agentGoalCols[a];
      if (agentGoalRow != -1
          && !this.isContainedInWalls(agentGoalRow, agentGoalCol, visitedCells, stack)) {
        throw new ParseException(String.format(
            "Agent '%s's goal cell is not in an area enclosed by walls.", (char) (a + '0')));
      }
    }

    for (int b = 0; b < this.levelInfo.numBoxes; ++b) {
      short boxRow = initialState.boxRows[b];
      short boxCol = initialState.boxCols[b];
      if (!this.isContainedInWalls(boxRow, boxCol, visitedCells, stack)) {
        throw new ParseException(
            String.format("Box '%s' at (%d, %d) is not in an area enclosed by walls.",
                (char) (this.levelInfo.boxLetters[b] + 'A'), boxRow, boxCol));
      }
    }

    for (int b = 0; b < this.levelInfo.numBoxGoals; ++b) {
      short boxGoalRow = this.levelInfo.boxGoalRows[b];
      short boxGoalCol = this.levelInfo.boxGoalCols[b];
      if (!this.isContainedInWalls(boxGoalRow, boxGoalCol, visitedCells, stack)) {
        throw new ParseException(
            String.format("Box goal '%s' at (%d, %d) is not in an area enclosed by walls.",
                (char) (this.levelInfo.boxLetters[b] + 'A'), boxGoalRow, boxGoalCol));
      }
    }

    // Treat unreachable cells as walls so they're not drawn when rendering states.
    visitedCells.flip(0, this.levelInfo.numRows * this.levelInfo.numCols);
    this.levelInfo.walls.or(visitedCells);
  }

  private boolean isContainedInWalls(short startRow, short startCol, BitSet visitedCells,
      LocationStack stack) {
    // For adjusting (row, col) to neighbour coordinates by accumulation in DFS.
    final short[] deltaRow = {-1, 2, -1, 0};
    final short[] deltaCol = {0, 0, -1, 2};

    if (visitedCells.get(startRow * this.levelInfo.numCols + startCol)) {
      return true;
    }

    // Reset stack, push this agent's location.
    stack.clear();
    stack.push(startRow, startCol);
    visitedCells.set(startRow * this.levelInfo.numCols + startCol);

    while (stack.size() > 0) {
      // Pop cell.
      short row = stack.topRow();
      short col = stack.topCol();
      stack.pop();

      // If wall cell, do nothing.
      if (this.levelInfo.walls.get(row * this.levelInfo.numCols + col)) {
        continue;
      }

      // If current cell is at level boundary, then agent is not in an area enclosed
      // by walls.
      if (row == 0 || row == this.levelInfo.numRows - 1 || col == 0
          || col == this.levelInfo.numCols - 1) {
        return false;
      }

      // Add unvisited neighbour cells to stack.
      for (int i = 0; i < 4; ++i) {
        row += deltaRow[i];
        col += deltaCol[i];
        if (!visitedCells.get(row * this.levelInfo.numCols + col)) {
          visitedCells.set(row * this.levelInfo.numCols + col);
          stack.push(row, col);
        }
      }
    }

    return true;
  }

  private String parseNameSection(LineNumberReader levelReader) throws IOException, ParseException {
    String line = levelReader.readLine();
    if (line == null) {
      throw new ParseException("Expected a level name, but reached end of file.",
          levelReader.getLineNumber());
    }
    if (line.isBlank()) {
      throw new ParseException("Level name can not be blank.", levelReader.getLineNumber());
    }
    this.levelInfo.levelName = line;
    line = levelReader.readLine();
    return line;
  }

  private String parseColorsSection(LineNumberReader levelReader)
      throws IOException, ParseException {
    while (true) {
      String line = levelReader.readLine();
      if (line == null) {
        throw new ParseException(
            "Expected more color lines or end of color section, but reached end of file.",
            levelReader.getLineNumber());
      }

      if (line.length() > 0 && line.charAt(0) == '#') {
        return line;
      }

      String[] split = line.split(":");
      if (split.length < 1) {
        throw new ParseException("Invalid color line syntax - missing a colon?",
            levelReader.getLineNumber());
      }
      if (split.length > 2) {
        throw new ParseException("Invalid color line syntax - too many colons?",
            levelReader.getLineNumber());
      }

      String colorName = split[0].strip().toLowerCase(java.util.Locale.ROOT);
      Color color = Colors.fromString(colorName);
      if (color == null) {
        throw new ParseException(String.format("Invalid color name: '%s'.", colorName),
            levelReader.getLineNumber());
      }

      String[] symbols = split[1].split(",");
      for (String symbol : symbols) {
        symbol = symbol.strip();
        if (symbol.isEmpty()) {
          throw new ParseException("Missing agent or box specifier between commas.",
              levelReader.getLineNumber());
        }
        if (symbol.length() > 1) {
          throw new ParseException(String.format("Invalid agent or box symbol: '%s'.", symbol),
              levelReader.getLineNumber());
        }
        char s = symbol.charAt(0);
        if ('0' <= s && s <= '9') {
          if (this.levelInfo.agentColors[s - '0'] != null) {
            throw new ParseException(String.format("Agent '%s' already has a color specified.", s),
                levelReader.getLineNumber());
          }
          this.levelInfo.agentColors[s - '0'] = color;
        } else if ('A' <= s && s <= 'Z') {
          if (this.levelInfo.boxColors[s - 'A'] != null) {
            throw new ParseException(String.format("Box '%s' already has a color specified.", s),
                levelReader.getLineNumber());
          }
          this.levelInfo.boxColors[s - 'A'] = color;
        } else {
          throw new ParseException(String.format("Invalid agent or box symbol: '%s'.", s),
              levelReader.getLineNumber());
        }
      }
    }
  }

  private String parseInitialSection(LineNumberReader levelReader)
      throws IOException, ParseException {
    int numWalls = 0;
    short[] wallRows = new short[1024];
    short[] wallCols = new short[1024];

    int numBoxes = 0;
    short[] boxRows = new short[128];
    short[] boxCols = new short[128];
    byte[] boxLetters = new byte[128];

    short[] agentRows = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
    short[] agentCols = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

    // Parse level and accumulate walls, agents, and boxes.
    String line;
    while (true) {
      line = levelReader.readLine();
      if (line == null) {
        throw new ParseException(
            "Expected more initial state lines or end of initial state section, but reached end of file.",
            levelReader.getLineNumber());
      }

      if (line.length() > 0 && line.charAt(0) == '#') {
        break;
      }

      line = this.stripTrailingSpaces(line);

      if (line.length() > Short.MAX_VALUE) {
        throw new ParseException(
            String.format("Initial state too large. Width greater than %s.", Short.MAX_VALUE),
            levelReader.getLineNumber());
      }

      if (line.length() >= this.levelInfo.numCols) {
        this.levelInfo.numCols = (short) line.length();
      }

      for (short col = 0; col < line.length(); ++col) {
        char c = line.charAt(col);
        // noinspection StatementWithEmptyBody
        if (c == ' ') {
          // Free cell.
        } else if (c == '+') {
          // Wall.
          if (numWalls == wallRows.length) {
            wallRows = Arrays.copyOf(wallRows, wallRows.length * 2);
            wallCols = Arrays.copyOf(wallCols, wallCols.length * 2);
          }
          wallRows[numWalls] = this.levelInfo.numRows;
          wallCols[numWalls] = col;
          ++numWalls;
        } else if ('0' <= c && c <= '9') {
          // Agent.
          if (agentRows[c - '0'] != -1) {
            throw new ParseException(
                String.format("Agent '%s' appears multiple times in initial state.", c),
                levelReader.getLineNumber());
          }
          if (this.levelInfo.agentColors[c - '0'] == null) {
            throw new ParseException(String.format("Agent '%s' has no color specified.", c),
                levelReader.getLineNumber());
          }
          agentRows[c - '0'] = this.levelInfo.numRows;
          agentCols[c - '0'] = col;
        } else if ('A' <= c && c <= 'Z') {
          // Box.
          if (this.levelInfo.boxColors[c - 'A'] == null) {
            throw new ParseException(String.format("Box '%s' has no color specified.", c),
                levelReader.getLineNumber());
          }
          if (numBoxes == boxRows.length) {
            boxRows = Arrays.copyOf(boxRows, boxRows.length * 2);
            boxCols = Arrays.copyOf(boxCols, boxCols.length * 2);
            boxLetters = Arrays.copyOf(boxLetters, boxLetters.length * 2);
          }
          boxRows[numBoxes] = this.levelInfo.numRows;
          boxCols[numBoxes] = col;
          boxLetters[numBoxes] = (byte) (c - 'A');
          ++numBoxes;
        } else {
          throw new ParseException(String.format("Invalid character '%s' in column %s.", c, col),
              levelReader.getLineNumber());
        }
      }

      ++this.levelInfo.numRows;
      if (this.levelInfo.numRows < 0) {
        throw new ParseException(
            String.format("Initial state too large. Height greater than %s.", Short.MAX_VALUE),
            levelReader.getLineNumber());
      }
    }

    // Count the agents; ensure that they are numbered consecutively.
    for (byte a = 0; a < 10; ++a) {
      if (agentRows[a] != -1) {
        if (this.levelInfo.numAgents == a) {
          ++this.levelInfo.numAgents;
        } else {
          throw new ParseException("Agents must be numbered consecutively.",
              levelReader.getLineNumber());
        }
      }
    }
    if (this.levelInfo.numAgents == 0) {
      throw new ParseException("Level contains no agents.", levelReader.getLineNumber());
    }

    // Set walls.
    this.levelInfo.walls = new BitSet(this.levelInfo.numRows * this.levelInfo.numCols);
    for (int w = 0; w < numWalls; ++w) {
      this.levelInfo.walls.set(wallRows[w] * this.levelInfo.numCols + wallCols[w]);
    }

    // Set box information.
    this.levelInfo.numBoxes = numBoxes;
    this.levelInfo.boxLetters = Arrays.copyOf(boxLetters, this.levelInfo.numBoxes);
    this.levelInfo.sortedBoxIds = new int[this.levelInfo.numBoxes];
    for (int i = 0; i < this.levelInfo.numBoxes; ++i) {
      this.levelInfo.sortedBoxIds[i] = i;
    }

    // Create initial state.
    State initialState = new State(Arrays.copyOf(boxRows, numBoxes),
        Arrays.copyOf(boxCols, numBoxes), Arrays.copyOf(agentRows, this.levelInfo.numAgents),
        Arrays.copyOf(agentCols, this.levelInfo.numAgents));

    this.levelInfo.initialState = initialState;

    return line;
  }

  private String parseGoalSection(LineNumberReader levelReader) throws IOException, ParseException {
    int numBoxGoals = 0;
    short[] boxGoalRows = new short[128];
    short[] boxGoalCols = new short[128];
    byte[] boxGoalLetters = new byte[128];

    this.levelInfo.agentGoalRows = new short[this.levelInfo.numAgents];
    Arrays.fill(this.levelInfo.agentGoalRows, (short) -1);
    this.levelInfo.agentGoalCols = new short[this.levelInfo.numAgents];
    Arrays.fill(this.levelInfo.agentGoalCols, (short) -1);

    short row = 0;

    String line;
    while (true) {
      line = levelReader.readLine();
      if (line == null) {
        throw new ParseException(
            "Expected more goal state lines or end of goal state section, but reached end of file.",
            levelReader.getLineNumber());
      }

      if (line.length() > 0 && line.charAt(0) == '#') {
        if (row != this.levelInfo.numRows) {
          throw new ParseException(
              "Goal state must have the same number of rows as the initial state, but has too few.",
              levelReader.getLineNumber());
        }
        break;
      }

      if (row == this.levelInfo.numRows) {
        throw new ParseException(
            "Goal state must have the same number of rows as the initial state, but has too many.",
            levelReader.getLineNumber());
      }

      line = this.stripTrailingSpaces(line);

      if (line.length() > Short.MAX_VALUE) {
        throw new ParseException(
            String.format("Goal state too large. Width greater than %s.", Short.MAX_VALUE),
            levelReader.getLineNumber());
      }

      if (line.length() > this.levelInfo.numCols) {
        throw new ParseException("Goal state can not have more columns than the initial state.",
            levelReader.getLineNumber());
      }

      short col = 0;
      for (; col < line.length(); ++col) {
        char c = line.charAt(col);
        if (c == '+') {
          // Wall.
          if (!this.levelInfo.wallAt(row, col)) {
            // Which doesn't match a wall in the initial state.
            throw new ParseException(
                String.format("Initial state has no wall at column %d, but goal state does.", col),
                levelReader.getLineNumber());
          }
        } else if (this.levelInfo.wallAt(row, col)) // Implicitly c != '+' from first if check.
        {
          // Missing wall compared to the initial state.
          throw new ParseException(
              String.format("Goal state not matching initial state's wall on column %d.", col),
              levelReader.getLineNumber());
        } else if (c == ' ') {
          // Free cell.
        } else if ('0' <= c && c <= '9') {
          // Agent.
          if (c - '0' >= this.levelInfo.numAgents) {
            throw new ParseException(String
                .format("Goal state has agent '%s' who does not appear in the initial state.", c),
                levelReader.getLineNumber());
          }
          if (this.levelInfo.agentGoalRows[c - '0'] != -1) {
            throw new ParseException(
                String.format("Agent '%s' appears multiple times in goal state.", c),
                levelReader.getLineNumber());
          }
          this.levelInfo.agentGoalRows[c - '0'] = row;
          this.levelInfo.agentGoalCols[c - '0'] = col;
        } else if ('A' <= c && c <= 'Z') {
          // Box.
          if (numBoxGoals == boxGoalRows.length) {
            boxGoalRows = Arrays.copyOf(boxGoalRows, boxGoalRows.length * 2);
            boxGoalCols = Arrays.copyOf(boxGoalCols, boxGoalCols.length * 2);
            boxGoalLetters = Arrays.copyOf(boxGoalLetters, boxGoalLetters.length * 2);
          }
          boxGoalRows[numBoxGoals] = row;
          boxGoalCols[numBoxGoals] = col;
          boxGoalLetters[numBoxGoals] = (byte) (c - 'A');
          ++numBoxGoals;
        } else {
          throw new ParseException(String.format("Invalid character '%s' in column %s.", c, col),
              levelReader.getLineNumber());
        }
      }
      // If the goal state line is shorter than the level width, we must check that no
      // walls were
      // omitted.
      for (; col < this.levelInfo.numCols; ++col) {
        if (this.levelInfo.wallAt(row, col)) {
          throw new ParseException(
              String.format("Goal state not matching initial state's wall on column %s.", col),
              levelReader.getLineNumber());
        }
      }
      ++row;
    }

    this.levelInfo.numBoxGoals = numBoxGoals;
    this.levelInfo.boxGoalRows = Arrays.copyOf(boxGoalRows, numBoxGoals);
    this.levelInfo.boxGoalCols = Arrays.copyOf(boxGoalCols, numBoxGoals);
    this.levelInfo.boxGoalLetters = Arrays.copyOf(boxGoalLetters, numBoxGoals);

    return line;
  }

  private String parseClientNameSection(LineNumberReader levelReader)
      throws IOException, ParseException {
    String line = levelReader.readLine();
    if (line == null) {
      throw new ParseException("Expected a client name, but reached end of file.",
          levelReader.getLineNumber());
    }
    if (line.isBlank()) {
      throw new ParseException("Client name can not be blank.", levelReader.getLineNumber());
    }
    this.levelInfo.clientName = line;
    line = levelReader.readLine();
    return line;
  }

  private String parseActionsSection(LineNumberReader levelReader)
      throws IOException, ParseException {
    Action[] jointAction = new Action[this.levelInfo.numAgents];

    while (true) {
      String line = levelReader.readLine();
      if (line == null) {
        throw new ParseException(
            "Expected more action lines or end of actions section, but reached end of " + "file.",
            levelReader.getLineNumber());
      }

      if (line.length() > 0 && line.charAt(0) == '#') {
        return line;
      }

      String[] split = line.split(":");
      if (split.length < 1) {
        throw new ParseException("Invalid action line syntax - timestamp missing?",
            levelReader.getLineNumber());
      }
      if (split.length > 2) {
        throw new ParseException("Invalid action line syntax - too many colons?",
            levelReader.getLineNumber());
      }

      // Parse action timestamp.
      long actionTime;
      try {
        actionTime = Long.parseLong(split[0]);
      } catch (NumberFormatException e) {
        throw new ParseException("Invalid action timestamp.", levelReader.getLineNumber());
      }

      // Parse and execute joint action.
      String[] actionsStr = split[1].split("\\|");
      if (actionsStr.length != this.levelInfo.numAgents) {
        throw new ParseException("Invalid number of agents in joint action.",
            levelReader.getLineNumber());
      }
      for (int i = 0; i < jointAction.length; ++i) {
        jointAction[i] = Action.parse(actionsStr[i]);
        if (jointAction[i] == null) {
          throw new ParseException("Invalid joint action.", levelReader.getLineNumber());
        }
      }

      // Execute action.
      State state = this.stateSequence.getState(this.stateSequence.getNumStates() - 1);
      boolean[] applicable = this.validator.isApplicable(jointAction, state);
      this.stateSequence.apply(jointAction, applicable, actionTime);
    }
  }

  private String parseSolvedSection(LineNumberReader levelReader)
      throws IOException, ParseException {
    String line = levelReader.readLine();
    if (line == null) {
      throw new ParseException("Expected a solved value, but reached end of file.",
          levelReader.getLineNumber());
    }

    if (!line.equals("true") && !line.equals("false")) {
      throw new ParseException("Invalid solved value.", levelReader.getLineNumber());
    }

    boolean logSolved = line.equals("true");
    boolean actuallySolved = true;
    for (int boxGoalId = 0; boxGoalId < this.levelInfo.numBoxGoals; ++boxGoalId) {
      short boxGoalRow = this.levelInfo.boxGoalRows[boxGoalId];
      short boxGoalCol = this.levelInfo.boxGoalCols[boxGoalId];
      byte boxGoalLetter = this.levelInfo.boxGoalLetters[boxGoalId];

      if (this.stateSequence.boxAt(boxGoalRow, boxGoalCol) != boxGoalLetter) {
        actuallySolved = false;
        break;
      }
    }
    State lastState = this.stateSequence.getState(this.stateSequence.getNumStates() - 1);
    for (int agent = 0; agent < this.levelInfo.numAgents; ++agent) {
      short agentGoalRow = this.levelInfo.agentGoalRows[agent];
      short agentGoalCol = this.levelInfo.agentGoalCols[agent];
      short agentRow = lastState.agentRows[agent];
      short agentCol = lastState.agentCols[agent];

      if (agentGoalRow != -1 && (agentRow != agentGoalRow || agentCol != agentGoalCol)) {
        actuallySolved = false;
        break;
      }
    }

    if (logSolved && !actuallySolved) {
      throw new ParseException(
          "Log summary claims level is solved, but the actions don't solve the level.",
          levelReader.getLineNumber());
    } else if (!logSolved && actuallySolved) {
      throw new ParseException(
          "Log summary claims level is not solved, but the actions solve the level.",
          levelReader.getLineNumber());
    }

    line = levelReader.readLine();
    return line;
  }

  private String parseNumActionsSection(LineNumberReader levelReader)
      throws IOException, ParseException {
    String line = levelReader.readLine();
    if (line == null) {
      throw new ParseException("Expected a solved value, but reached end of file.",
          levelReader.getLineNumber());
    }

    long numActions;
    try {
      numActions = Long.parseLong(line);
    } catch (NumberFormatException e) {
      throw new ParseException("Invalid number of actions.", levelReader.getLineNumber());
    }

    if (numActions != this.stateSequence.getNumStates() - 1) {
      throw new ParseException(
          "Number of action does not conform to the number of actions in the sequence.",
          levelReader.getLineNumber());
    }

    line = levelReader.readLine();
    return line;
  }

  private String parseTimeSection(LineNumberReader levelReader) throws IOException, ParseException {
    String line = levelReader.readLine();
    if (line == null) {
      throw new ParseException("Expected a solved value, but reached end of file.",
          levelReader.getLineNumber());
    }

    long lastStateTime;
    try {
      lastStateTime = Long.parseLong(line);
    } catch (NumberFormatException e) {
      throw new ParseException("Invalid time of last action.", levelReader.getLineNumber());
    }

    if (lastStateTime != this.stateSequence.getStateTime(this.stateSequence.getNumStates() - 1)) {
      throw new ParseException(
          "Last state time does not conform to the timestamp of the last action.",
          levelReader.getLineNumber());
    }

    line = levelReader.readLine();
    return line;
  }

  private String parseEndSection(LineNumberReader levelReader) throws IOException, ParseException {
    return levelReader.readLine();
  }

  private String stripTrailingSpaces(String s) {
    int endIndex = s.length();
    while (endIndex > 0 && s.charAt(endIndex - 1) == ' ') {
      --endIndex;
    }
    return s.substring(0, endIndex);
  }
}
