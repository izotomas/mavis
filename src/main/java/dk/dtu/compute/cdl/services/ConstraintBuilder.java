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

import java.util.AbstractMap.SimpleEntry;
import dk.dtu.compute.cdl.enums.OperandType;
import dk.dtu.compute.cdl.enums.PredicateParsingStateMachine;
import dk.dtu.compute.cdl.model.Action;
import dk.dtu.compute.cdl.model.ActionContext;
import dk.dtu.compute.cdl.model.Connector;
import dk.dtu.compute.cdl.model.Constraint;
import dk.dtu.compute.cdl.model.Expression;
import dk.dtu.compute.cdl.model.Operand;
import dk.dtu.compute.cdl.model.Operator;

public class ConstraintBuilder {
  class ContextEntry {
    public String key;
    public Action entry;

    public boolean isValid() {
      return this.key != null && !this.key.isEmpty() && this.entry != null;
    }
  }

  private PredicateParsingStateMachine predicateSM;
  private Expression current;

  protected Expression expression;
  protected ContextEntry requestingContext;
  protected ContextEntry restrictingContext;

  public ConstraintBuilder() {
    this.predicateSM = PredicateParsingStateMachine.initialize();
    this.requestingContext = new ContextEntry();
    this.restrictingContext = new ContextEntry();
    this.expression = new Expression();
    this.current = this.expression;
  }

  public boolean isSingleContextConstraint() {
    if (!this.requestingContext.isValid()) {
      throw new IllegalStateException("Must provide context mapping first.");
    }
    return this.restrictingContext.key == null;
  }

  public Constraint build() throws IllegalStateException {
    validate();

    var context = new ActionContext()
        .mapContext(new SimpleEntry<>(this.requestingContext.key, this.requestingContext.entry));

    if (this.restrictingContext.isValid()) {
      context.mapContext(
          new SimpleEntry<>(this.restrictingContext.key, this.restrictingContext.entry));
    }

    var predicate = expression.toPredicate();

    return new Constraint(context, predicate);
  }

  public ConstraintBuilder withRequestingContext(Action context) {
    if (context == null) {
      throw new IllegalArgumentException("Action context may not be null.");
    }
    this.requestingContext.entry = context;
    return this;
  }

  protected ConstraintBuilder withRequestingContextName(String name) {
    if (name == null || name.isEmpty() || name == this.restrictingContext.key) {
      throw new IllegalArgumentException(
          String.format("Context key must be unique and non null.\n\tKey: %s", name));
    }
    this.requestingContext.key = name;
    return this;
  }

  public ConstraintBuilder withRestrictingContext(Action context) {
    if (context == null) {
      throw new IllegalArgumentException("Action context may not be null.");
    }
    this.restrictingContext.entry = context;
    return this;
  }

  protected ConstraintBuilder withRestrictingContextName(String name) {
    if (name == null || name.isEmpty() || name == this.requestingContext.key) {
      throw new IllegalArgumentException(
          String.format("Context key must be unique and non null.\n\tKey: %s", name));
    }
    this.restrictingContext.key = name;
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
    if (!requestingContext.isValid()) {
      throw new IllegalStateException("Missing required requesting context");
    }
    if (restrictingContext.key != null && !restrictingContext.isValid()) {
      throw new IllegalStateException("Missing required restricting context");
    }

    // contextual variables
    var curr = expression;
    while (curr != null) {
      for (var operand : new Operand[] {curr.operand1, curr.operand2}) {
        if (operand.type != OperandType.ActionReference) {
          continue;
        }

        if (!operand.actionKey.equals(requestingContext.key)
            && !operand.actionKey.equals(restrictingContext.key)) {
          throw new IllegalStateException(
              String.format("Missing required action context entry: %s", operand.actionKey));
        }
      }
      curr = curr.next();
    }
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
