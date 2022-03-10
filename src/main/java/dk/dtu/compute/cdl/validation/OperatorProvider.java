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
package dk.dtu.compute.cdl.validation;

import java.lang.reflect.Method;
import org.javatuples.*;


public class OperatorProvider {
  public boolean is(Integer one, Integer other) {
    return one == other;
  }

  public boolean is(String one, String other) {
    return one.equals(other);
  }

  public boolean is(Pair<Integer, Integer> one, Pair<Integer, Integer> other) {
    return one.equals(other);
  }

  public boolean less(Integer one, Integer other) {
    return one < other;
  }

  public boolean more(Integer one, Integer other) {
    return one > other;
  }

  public boolean overlaps(Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> one,
      Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> other) {
    return is(one.getValue0(), other.getValue0()) || is(one.getValue1(), other.getValue0())
        || is(one.getValue0(), other.getValue1()) || is(one.getValue1(), other.getValue1());
  }

  public boolean evaluate(String operatorName, Object arg1, Object arg2)
      throws IllegalArgumentException {
    Method operator = null;
    switch (operatorName) {
      case "IS":
        operator = getMethod("is", arg1, arg2);
        return evaluate(operator, arg1, arg2);
      case "IS NOT":
        operator = getMethod("is", arg1, arg2);
        return !evaluate(operator, arg1, arg2);

      default:
        throw new IllegalArgumentException(
            String.format("Undefined function: '%s'\n\tArg1Class: %s\n\tArg2Class: %s",
                operatorName, arg1.getClass(), arg2.getClass()));
    }
  }

  protected boolean evaluate(Method operatorMethod, Object arg1, Object arg2)
      throws IllegalArgumentException {
    try {
      return (boolean) operatorMethod.invoke(this, arg1, arg2);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  private Method getMethod(String operator, Object arg1, Object arg2)
      throws IllegalArgumentException {
    try {
      var method = this.getClass().getMethod(operator, arg1.getClass(), arg2.getClass());
      return method;
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }
}
