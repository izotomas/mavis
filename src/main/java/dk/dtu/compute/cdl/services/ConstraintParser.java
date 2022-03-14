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
package dk.dtu.compute.cdl.services;

import java.util.regex.Pattern;
import dk.dtu.compute.cdl.errors.StatementParsingException;

public class ConstraintParser {

  private final String[] EXPRESSION_BLOCKS =
      new String[] {"connector", "operand1", "operator", "operand2"};


  public ConstraintBuilder builder;
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
          // requesting context entry
          + "(?<requesting>[a-z]+)"
          // is blocked | is blocked by action
          + "\\hIS\\hBLOCKED(?:\\hBY\\hACTION\\h"
          // restricting context entry
          + "(?<restricting>[a-z]+)"
          // end
          + ")?$");

  private final static Pattern predicateValidationPattern = Pattern.compile(
      // start
      "^(?:"
          // predicate connector
          + "(?:\\h(?:(?:AND|OR)(?:\\hNOT)?)\\h|^)"
          // operand1
          + "(?:[a-z]+\\.[a-z]+|\\'[\\w\\h().,*]+\\'|[0-9]+)"
          // operator
          + "\\h(?:[A-Z]+(?:\\h[A-Z]+)*)\\h"
          // operand2
          + "(?:[a-z]+\\.[a-z]+|\\'[\\w\\h().,*]+\\'|[0-9]+)"
          // repeat until end
          + ")+$");

  private final static Pattern predicateExtractorPattern = Pattern.compile(
      // predicate connector
      "(?:\\h(?<connector>(?:AND|OR)(?:\\hNOT)?)\\h|^)"
          // predicate operands and operator
          + "(?<operand1>[a-z]+\\.[a-z]+|\\'[\\w\\h().,*]+\\'|[0-9]+)"
          + "\\h(?<operator>[A-Z]+(?:\\h[A-Z]+)*)\\h"
          + "(?<operand2>[a-z]+\\.[a-z]+|\\'[\\w()\\h.,*]+\\'|[0-9]+)");



  public ConstraintParser() {
    this.builder = new ConstraintBuilder();
  }

  public ConstraintBuilder Parse(String constraintDefinition) throws StatementParsingException {
    this.builder = new ConstraintBuilder();
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
    var matcher = ConstraintParser.constraintPattern.matcher(constraintDefinition);
    if (!matcher.matches() || matcher.group("context") == null
        || matcher.group("predicate") == null) {
      throw new StatementParsingException(String.format(
          "Constraint definition does not match the required pattern.\n\tStatement: %s\n\tPattern: %s",
          constraintDefinition, ConstraintParser.constraintPattern.pattern()));
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
    var matcher = ConstraintParser.contextPattern.matcher(context);
    if (!matcher.matches()) {
      throw new StatementParsingException(String.format(
          "Context definition does not match the required pattern.\n\tStatement: %s\n\tPattern: %s",
          context, ConstraintParser.contextPattern.pattern()));
    }
    String contextName;
    if ((contextName = matcher.group("requesting")) != null) {
      builder.withRequestingContextName(contextName);
    }
    if ((contextName = matcher.group("restricting")) != null) {
      builder.withRestrictingContextName(contextName);
    }
  }

  protected void ParsePredicate(String predicate) throws StatementParsingException {
    var matcher = ConstraintParser.predicateValidationPattern.matcher(predicate);
    if (!matcher.matches()) {
      throw new StatementParsingException(String.format(
          "Predicate definition does not match the required pattern.\n\tPredicate: %s\n\tPattern: %s",
          predicate, ConstraintParser.predicateValidationPattern.pattern()));
    }
    matcher = ConstraintParser.predicateExtractorPattern.matcher(predicate);
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
                  predicate, ConstraintParser.predicateExtractorPattern.pattern()));
        }
        builder.withPredicateToken(token);
      }
    }
  }
}
