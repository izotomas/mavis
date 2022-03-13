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

import java.lang.reflect.Method;
import org.javatuples.*;


public final class OperatorProvider {

  public static boolean is(Integer one, Integer other) {
    return one == other;
  }

  public static boolean is(String one, String other) {
    return one.equals(other);
  }

  public static boolean is(Pair<Integer, Integer> one, Pair<Integer, Integer> other) {
    return one.equals(other);
  }

  public static boolean less(Integer one, Integer other) {
    return one < other;
  }

  public static boolean more(Integer one, Integer other) {
    return one > other;
  }

  public static boolean overlaps(Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> one,
      Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> other) {
    return is(one.getValue0(), other.getValue0()) || is(one.getValue1(), other.getValue0())
        || is(one.getValue0(), other.getValue1()) || is(one.getValue1(), other.getValue1());
  }
}
