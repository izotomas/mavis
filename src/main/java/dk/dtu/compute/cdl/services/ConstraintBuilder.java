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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import java.util.regex.Pattern;
import dk.dtu.compute.cdl.enums.OperandType;
import dk.dtu.compute.cdl.enums.OperandValueType;
import dk.dtu.compute.cdl.enums.OperatorType;
import dk.dtu.compute.cdl.model.Action;
import dk.dtu.compute.cdl.model.ActionContext;
import dk.dtu.compute.cdl.model.Constraint;
import dk.dtu.compute.cdl.model.Expression;
import dk.dtu.compute.cdl.model.Operand;
import dk.dtu.compute.cdl.model.Operator;

public class ConstraintBuilder {

  private enum PredicateParsingState {
    OPERAND1, OPERATOR, OPERAND2, CONNECTOR,
  }

  private final Pattern actionReferencePattern = Pattern.compile("^(?<context>[a-z]+)\\.[a-z]+$");
  private final static Set<String> ALLOWED_CONTEXT_KEYS = Set.of("entry1", "entry2");
  private final static Map<OperatorType, Set<OperandValueType>> OPERATOR_ARGS_MAP = Map.ofEntries(
      new SimpleEntry<>(OperatorType.IS,
          Set.of(OperandValueType.Number, OperandValueType.String, OperandValueType.Vertex)),
      new SimpleEntry<>(OperatorType.LESS, Set.of(OperandValueType.Number)),
      new SimpleEntry<>(OperatorType.MORE, Set.of(OperandValueType.Number)),
      new SimpleEntry<>(OperatorType.OVERLAPS, Set.of(OperandValueType.Edge)));

  private PredicateParsingState predicateStateMachine;
  private Expression current;

  protected final HashMap<String, String> contextMap;

  protected Expression expression;
  protected Action contextEntry1;
  protected Action contextEntry2;

  public ConstraintBuilder() {
    this.predicateStateMachine = PredicateParsingState.OPERAND1;
    this.expression = new Expression();
    this.contextMap = new HashMap<>();
    this.current = this.expression;
  }

  public Constraint build() throws IllegalStateException {
    validate();

    var context = new ActionContext()
        .mapContext(new SimpleEntry<>(this.contextMap.get("entry1"), this.contextEntry1));
    if (this.contextEntry2 != null) {
      context.mapContext(new SimpleEntry<>(this.contextMap.get("entry2"), this.contextEntry2));
    }


    var curr = expression;
    var predicate = curr.toPredicate();
    while (curr.hasNext()) {
      curr = curr.next();
      if (curr.connector.equals("AND")) {
        predicate = predicate.and(curr.toPredicate());
      }
    }

    return new Constraint(context, predicate);
  }

  public ConstraintBuilder withRequestingActionContext(Action action) {
    if (action == null) {
      throw new IllegalArgumentException("Action context may not be null.");
    }
    this.contextEntry1 = action;
    return this;
  }

  public ConstraintBuilder withBlockingActionContext(Action action) {
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
    var curr = expression;
    while (curr.hasNext()) {
      if (curr.operand1.type == OperandType.ActionReference) {
        var value = (String) curr.operand1.value;
        var matcher = actionReferencePattern.matcher(value);
        matcher.find();
        var contextEntry = matcher.group("context");
        if (!contextMap.values().contains(contextEntry)) {
          throw new IllegalStateException(
              String.format("Missing required action context entry: %s", contextEntry));
        }
      }
      curr = curr.next();
    }
  }

  protected ConstraintBuilder withContextMapping(SimpleEntry<String, String> entry)
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

  protected ConstraintBuilder withPredicateToken(String token) throws IllegalArgumentException {
    if (token == null || token.isEmpty()) {
      throw new IllegalArgumentException("Predicate token may not be null or empty.");
    }
    switch (predicateStateMachine) {
      case OPERAND1:
        current.operand1 = new Operand(token);
        predicateStateMachine = PredicateParsingState.OPERATOR;
        break;
      case OPERATOR:
        current.operator = new Operator(token);

        if (!OPERATOR_ARGS_MAP.get(current.operator.type).contains(current.operand1.valueType)) {
          throw new IllegalArgumentException(
              String.format("Operator %s does not support operand1 of type: %s", token,
                  current.operand1.valueType));
        }

        predicateStateMachine = PredicateParsingState.OPERAND2;
        break;
      case OPERAND2:
        current.operand2 = new Operand(token);
        predicateStateMachine = PredicateParsingState.CONNECTOR;

        if (!OPERATOR_ARGS_MAP.get(current.operator.type).contains(current.operand2.valueType)) {
          throw new IllegalArgumentException(
              String.format("Operator %s does not support operand2 of type: %s", token,
                  current.operand1.valueType));
        }

        if (current.operand1.valueType != current.operand2.valueType) {
          throw new IllegalArgumentException(
              String.format("Operands are of incompatible type.\n\tOperand1: %s.\n\tOperand2: %s",
                  current.operand1.valueType, current.operand2.valueType));
        }

        break;
      case CONNECTOR:
        current = new Expression(current);
        current.connector = token;
        predicateStateMachine = PredicateParsingState.OPERAND1;
        break;
    }
    return this;
  }
}
