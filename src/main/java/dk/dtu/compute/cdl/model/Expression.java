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
package dk.dtu.compute.cdl.model;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.function.Predicate;
import dk.dtu.compute.cdl.enums.OperandType;
import dk.dtu.compute.cdl.errors.PredicateExecutionFailedException;
import dk.dtu.compute.cdl.services.OperatorProvider;

public final class Expression implements Iterator<Expression> {
  public Operand operand1;
  public Operator operator;
  public Operand operand2;
  public String connector;

  private Expression next;

  public Expression() {}

  public Expression(Expression parent) {
    parent.next = this;
  }

  public boolean hasNext() {
    return next != null;
  }

  public Expression next() {
    return next;
  }

  public Predicate<ValidationContext> toPredicate() {
    return context -> operator.predicate.test(getArg(operand1, context), getArg(operand2, context));
  }

  private Object getArg(Operand operand, ValidationContext context) {
    return operand.type == OperandType.Literal ? operand.value
        : context.get((String) operand.value);
  }
}
