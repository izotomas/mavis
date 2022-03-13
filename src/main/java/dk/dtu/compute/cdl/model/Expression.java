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

import java.util.Iterator;
import java.util.function.Predicate;

public final class Expression implements Iterator<Expression> {
  public Operand operand1;
  public Operator operator;
  public Operand operand2;
  public Connector connector;

  private Expression next;

  public Expression() {}

  public Expression(Expression parent) {
    parent.next = this;
  }

  protected Expression(Operand operand1, Operator operator, Operand operand2) {
    this.operand1 = operand1;
    this.operator = operator;
    this.operand2 = operand2;
  }

  public boolean hasNext() {
    return next != null;
  }

  public Expression next() {
    return next;
  }

  public Predicate<ValidationContext> toPredicate() {
    return context -> operator.predicate.test(operand1.getValue(context),
        operand2.getValue(context));
  }
}
