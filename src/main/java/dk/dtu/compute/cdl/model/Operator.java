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

import java.util.AbstractMap.SimpleEntry;
import java.util.function.BiPredicate;
import org.javatuples.Pair;
import java.lang.reflect.Method;
import java.util.Map;
import dk.dtu.compute.cdl.enums.OperandValueType;
import dk.dtu.compute.cdl.enums.OperatorType;
import dk.dtu.compute.cdl.errors.PredicateExecutionFailedException;
import dk.dtu.compute.cdl.services.OperatorProvider;

public class Operator {

  private static final Map<OperandValueType, Class<?>> FUNCTION_MAP =
      Map.ofEntries(new SimpleEntry<>(OperandValueType.Edge, Pair.class),
          new SimpleEntry<>(OperandValueType.Vertex, Pair.class),
          new SimpleEntry<>(OperandValueType.String, String.class),
          new SimpleEntry<>(OperandValueType.Number, Integer.class));

  private final Method method;
  public final OperatorType type;
  public BiPredicate<Object, Object> predicate;

  public Operator(String operatorString, OperandValueType argType) {
    try {
      this.type = OperatorType.fromString(operatorString);
      this.method = getMethod(type, argType);

      this.predicate = (arg1, arg2) -> {
        try {
          return (boolean) this.method.invoke(null, arg1, arg2);
        } catch (Exception e) {
          throw new PredicateExecutionFailedException(e.getMessage());
        }
      };
      if (operatorString.contains("NOT")) {
        this.predicate = this.predicate.negate();
      }
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(String
          .format("Operator %s does not support operand1 of type: %s", operatorString, argType));
    }
  }

  private static Method getMethod(OperatorType type, OperandValueType argType)
      throws NoSuchMethodException {
    var arg = FUNCTION_MAP.get(argType);
    return OperatorProvider.class.getMethod(toMethodName(type), arg, arg);
  }

  private static String toMethodName(OperatorType type) {
    return type.toString().toLowerCase();
  }
}
