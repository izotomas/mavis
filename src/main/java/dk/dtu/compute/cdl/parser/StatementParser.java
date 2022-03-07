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
package dk.dtu.compute.cdl.parser;

import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import java.util.regex.Pattern;

public class StatementParser {

  private final Set<String> ALLOWED_CONTEXT_KEYS = Set.of("entry1", "entry2");
  private final String[] EXPRESSION_BLOCKS =
      new String[] {"connector", "operand1", "operator", "operand2"};


  public StatementBuilder builder;
  public String contextString;
  public String predicateString;

  private final static Pattern constraintPattern = Pattern.compile(
      // start
      "^"
          // context
          + "(?<context>[a-zA-Z\\h]+)"
          // separator
          + "\\hIF\\h"
          // predicate
          + "(?<predicate>[\\w\\h.,*()'']+)"
          // end
          + "$");

  private final static Pattern contextPattern = Pattern.compile(
      // start
      "^ACTION\\h"
          // first context entry
          + "(?<entry1>[a-z]+)"
          // is blocked | is blocked by action
          + "\\hIS\\hBLOCKED(?:\\hBY\\hACTION\\h"
          // second context entry (optional)
          + "(?<entry2>[a-z]+)"
          // end
          + ")?$");

  private final static Pattern predicateValidationPattern = Pattern.compile(
      // start
      "^(?:"
          // predicate connector
          + "(?:\\h(?:(?:AND|OR)(?:\\hNOT)?)\\h|^)"
          // operand1
          + "(?:[a-z]+\\.[a-z]+|\\'[\\w().,*]+\\'|[0-9]+)"
          // operator
          + "\\h(?:[A-Z]+(?:\\h[A-Z]+)*)\\h"
          // operand2
          + "(?:[a-z]+\\.[a-z]+|\\'[\\w().,*]+\\'|[0-9]+)"
          // repeat until end
          + ")+$");

  private final static Pattern predicateExtractorPattern = Pattern.compile(
      // predicate connector
      "(?:\\h(?<connector>(?:AND|OR)(?:\\hNOT)?)\\h|^)"
          // predicate operands and operator
          + "(?<operand1>[a-z]+\\.[a-z]+|\\'[\\w().,*]+\\'|[0-9]+)"
          + "\\h(?<operator>[A-Z]+(?:\\h[A-Z]+)*)\\h"
          + "(?<operand2>[a-z]+\\.[a-z]+|\\'[\\w().,*]+\\'|[0-9]+)");


  public StatementParser() {
    this.builder = new StatementBuilder();
  }

  public StatementBuilder Parse(String constraintDefinition) throws StatementParsingException {
    this.builder = new StatementBuilder();
    this.contextString = null;
    this.predicateString = null;
    try {
      ParseInitial(constraintDefinition);
      ParseContext(this.contextString);
      ParsePredicate(this.predicateString);

      return builder;
    } catch (Exception e) {
      throw new StatementParsingException(e.getMessage());
    }
  }

  /**
   * Splits constraint definition into context and predicate
   * 
   * @param constraintDefinition
   * @throws StatementParsingException
   */
  protected void ParseInitial(String constraintDefinition) throws StatementParsingException {
    var matcher = StatementParser.constraintPattern.matcher(constraintDefinition);
    if (!matcher.matches() || matcher.group("context") == null
        || matcher.group("predicate") == null) {
      throw new StatementParsingException(String.format(
          "Constraint definition does not match the required pattern.\n\tStatement: %s\n\tPattern: %s",
          constraintDefinition, StatementParser.constraintPattern.pattern()));
    }

    contextString = matcher.group("context");
    predicateString = matcher.group("predicate");
  }

  /**
   * Sets the context mapping for the StatementBuilder
   * 
   * @param context
   * @throws StatementParsingException
   */
  protected void ParseContext(String context) throws StatementParsingException {
    var matcher = StatementParser.contextPattern.matcher(context);
    if (!matcher.matches()) {
      throw new StatementParsingException(String.format(
          "Context definition does not match the required pattern.\n\tStatement: %s\n\tPattern: %s",
          context, StatementParser.contextPattern.pattern()));
    }
    for (var groupName : ALLOWED_CONTEXT_KEYS) {
      var value = matcher.group(groupName);
      if (value != null) {
        builder.withContextMapping(new SimpleEntry<String, String>(groupName, value));
      }
    }
  }

  protected void ParsePredicate(String predicate) throws StatementParsingException {
    var matcher = StatementParser.predicateValidationPattern.matcher(predicate);
    if (!matcher.matches()) {
      throw new StatementParsingException(String.format(
          "Predicate definition does not match the required pattern.\n\tPredicate: %s\n\tPattern: %s",
          predicate, StatementParser.predicateValidationPattern.pattern()));
    }
    matcher = StatementParser.predicateExtractorPattern.matcher(predicate);
    var first = true;
    while (matcher.find()) {
      for (var groupName : EXPRESSION_BLOCKS) {
        if (first && groupName == "connector") {
          first = false;
          continue;
        }
        var token = matcher.group(groupName);
        if (token == null) {
          throw new StatementParsingException(
              String.format("Missing or invalid %s.\n\tPredicate: %s\n\tPattern: %s", groupName,
                  predicate, StatementParser.predicateExtractorPattern.pattern()));
        }
        builder.withPredicateToken(token);
      }
    }
  }
}
