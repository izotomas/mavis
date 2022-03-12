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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import java.util.regex.Pattern;
import dk.dtu.compute.cdl.model.Action;
import dk.dtu.compute.cdl.model.Expression;
import dk.dtu.compute.cdl.model.Operand;
import dk.dtu.compute.cdl.model.OperandType;
import dk.dtu.compute.cdl.model.OperandValueType;
import dk.dtu.compute.cdl.model.Operator;

public class StatementBuilder {

  private enum PredicateParsingState {
    OPERAND1, OPERATOR, OPERAND2, CONNECTOR,
  }

  private final Pattern actionReferencePattern = Pattern.compile("^(?<context>[a-z]+)\\.[a-z]+$");
  private final static Set<String> ALLOWED_CONTEXT_KEYS = Set.of("entry1", "entry2");
  private final static Map<Operator, Set<OperandValueType>> OPERATOR_ARGS_MAP = Map.ofEntries(
      new SimpleEntry<>(Operator.IS,
          Set.of(OperandValueType.Number, OperandValueType.String, OperandValueType.Vertex)),
      new SimpleEntry<>(Operator.LESS, Set.of(OperandValueType.Number)),
      new SimpleEntry<>(Operator.MORE, Set.of(OperandValueType.Number)),
      new SimpleEntry<>(Operator.OVERLAPS, Set.of(OperandValueType.Edge)));

  private PredicateParsingState predicateStateMachine;
  private Expression currentExpression;

  protected final HashMap<String, String> contextMap;

  protected Action contextEntry1;
  protected Action contextEntry2;
  protected final LinkedList<Expression> expressionList;

  public StatementBuilder() {
    this.predicateStateMachine = PredicateParsingState.OPERAND1;
    this.currentExpression = new Expression();
    this.contextMap = new HashMap<>();
    this.expressionList = new LinkedList<>();

    this.expressionList.add(currentExpression);
  }

  public void Build() throws IllegalStateException {
    validate();

  }

  public StatementBuilder withRequestingActionContext(Action action) {
    if (action == null) {
      throw new IllegalArgumentException("Action context may not be null.");
    }
    this.contextEntry1 = action;
    return this;
  }

  public StatementBuilder withBlockingActionContext(Action action) {
    if (action == null) {
      throw new IllegalArgumentException("Action context may not be null.");
    }
    this.contextEntry2 = action;
    return this;
  }

  /**
   * Validates context related properties
   * 
   * @throws IllegalStateException
   */
  protected void validate() throws IllegalStateException {
    if (!(predicateStateMachine == PredicateParsingState.OPERAND1
        || predicateStateMachine == PredicateParsingState.CONNECTOR)) {
      throw new IllegalStateException("Incomplete predicate");
    }
    var contexKeys = this.contextMap.keySet();
    if (contexKeys.size() == 0) {
      throw new IllegalStateException("Missing action context");
    } else if (contextEntry1 == null) {
      throw new IllegalStateException("Missing requesting action context");
    } else if (contexKeys.size() == 2 && contextEntry2 == null) {
      throw new IllegalStateException("Missing blocking action context");
    }

    // contextual variables
    for (var expression : expressionList) {
      if (expression.operand1.type == OperandType.ActionReference) {
        var value = (String) expression.operand1.value;
        var matcher = actionReferencePattern.matcher(value);
        matcher.find();
        var contextEntry = matcher.group("context");
        if (!contextMap.values().contains(contextEntry)) {
          throw new IllegalStateException(
              String.format("Missing required action context entry: %s", contextEntry));
        }
      }
    }
  }

  protected StatementBuilder withContextMapping(SimpleEntry<String, String> entry)
      throws IllegalArgumentException {
    var key = entry.getKey();
    var val = entry.getValue();
    if (key.isEmpty() || val.isEmpty() || this.contextMap.containsKey(key)) {
      throw new IllegalArgumentException(String
          .format("Context mapping must be unique and non null.\n\tKey: %s | Value: %s", key, val));
    }
    if (!ALLOWED_CONTEXT_KEYS.contains(key)) {
      throw new IllegalArgumentException(String.format("Invalid key %s.\n\tAllowed keys: %s", key,
          Arrays.toString(ALLOWED_CONTEXT_KEYS.toArray())));
    }
    this.contextMap.put(key, val);
    return this;
  }

  protected StatementBuilder withPredicateToken(String token) throws IllegalArgumentException {
    if (token == null || token.isEmpty()) {
      throw new IllegalArgumentException("Predicate token may not be null or empty.");
    }
    switch (predicateStateMachine) {
      case OPERAND1:
        currentExpression.operand1 = new Operand(token);
        predicateStateMachine = PredicateParsingState.OPERATOR;
        break;
      case OPERATOR:
        currentExpression.operator = Operator.fromString(token);

        if (!OPERATOR_ARGS_MAP.get(currentExpression.operator)
            .contains(currentExpression.operand1.valueType)) {
          throw new IllegalArgumentException(
              String.format("Operator %s does not support operand1 of type: %s", token,
                  currentExpression.operand1.valueType));
        }

        predicateStateMachine = PredicateParsingState.OPERAND2;
        break;
      case OPERAND2:
        currentExpression.operand2 = new Operand(token);
        predicateStateMachine = PredicateParsingState.CONNECTOR;

        if (!OPERATOR_ARGS_MAP.get(currentExpression.operator)
            .contains(currentExpression.operand2.valueType)) {
          throw new IllegalArgumentException(
              String.format("Operator %s does not support operand2 of type: %s", token,
                  currentExpression.operand1.valueType));
        }

        if (currentExpression.operand1.valueType != currentExpression.operand2.valueType) {
          throw new IllegalArgumentException(
              String.format("Operands are of incompatible type.\n\tOperand1: %s.\n\tOperand2: %s",
                  currentExpression.operand1.valueType, currentExpression.operand2.valueType));
        }

        break;
      case CONNECTOR:
        currentExpression.connector = token;
        currentExpression = new Expression();
        expressionList.add(currentExpression);
        predicateStateMachine = PredicateParsingState.OPERAND1;
        break;
    }
    return this;
  }
}
