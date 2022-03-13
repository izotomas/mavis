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
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import java.util.function.Predicate;
import dk.dtu.compute.cdl.enums.OperandType;
import dk.dtu.compute.cdl.enums.PredicateParsingStateMachine;
import dk.dtu.compute.cdl.model.Action;
import dk.dtu.compute.cdl.model.ActionContext;
import dk.dtu.compute.cdl.model.Connector;
import dk.dtu.compute.cdl.model.Constraint;
import dk.dtu.compute.cdl.model.Expression;
import dk.dtu.compute.cdl.model.Operand;
import dk.dtu.compute.cdl.model.Operator;
import dk.dtu.compute.cdl.model.ValidationContext;

public class ConstraintBuilder {

  private final static Set<String> ALLOWED_CONTEXT_KEYS = Set.of("entry1", "entry2");

  private PredicateParsingStateMachine predicateSM;
  private Expression current;

  protected final HashMap<String, String> contextMap;
  protected Expression expression;
  protected Action contextEntry1;
  protected Action contextEntry2;

  public ConstraintBuilder() {
    this.predicateSM = PredicateParsingStateMachine.initialize();
    this.contextMap = new HashMap<>();
    this.expression = new Expression();
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
    Predicate<ValidationContext> nextPredicate;
    while (curr.hasNext()) {
      curr = curr.next();
      nextPredicate = curr.toPredicate();
      if (curr.connector.negated) {
        nextPredicate = nextPredicate.negate();
      }

      switch (curr.connector.type) {
        case AND:
          predicate = predicate.and(nextPredicate);
        case OR:
          predicate = predicate.or(nextPredicate);
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
    if (!predicateSM.isEndingState()) {
      throw new IllegalStateException("Incomplete predicate");
    }
    var contexKeys = this.contextMap.keySet();
    if (contexKeys.size() == 0) {
      throw new IllegalStateException("Missing action context");

    } else if (contextEntry1 == null || this.contextMap.get("entry1") == null) {
      throw new IllegalStateException("Missing requesting action context");
    } else if (contexKeys.size() == 2
        && (contextEntry2 == null || this.contextMap.get("entry2") == null)) {
      throw new IllegalStateException("Missing blocking action context");
    }

    // contextual variables
    var curr = expression;
    while (curr != null) {
      validate(curr.operand1);
      validate(curr.operand2);
      curr = curr.next();
    }
  }

  private void validate(Operand operand) throws IllegalStateException {
    if (operand.type != OperandType.ActionReference) {
      return;
    }

    if (!contextMap.values().contains(operand.actionKey)) {
      throw new IllegalStateException(
          String.format("Missing required action context entry: %s", operand.actionKey));
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
    switch (predicateSM) {
      case OPERAND1:
        current.operand1 = new Operand(token);
        break;
      case OPERATOR:
        current.operator = new Operator(token, current.operand1.valueType);
        break;
      case OPERAND2:
        current.operand2 = new Operand(token);

        if (!current.operand2.compatibleWith(current.operand1))
          throw new IllegalArgumentException(
              String.format("Operands are of incompatible type.\n\tOperand1: %s.\n\tOperand2: %s",
                  current.operand1.valueType, current.operand2.valueType));
        break;
      case CONNECTOR:
        current = new Expression(current);
        current.connector = new Connector(token);
        break;
    }
    predicateSM = predicateSM.next();
    return this;
  }
}
