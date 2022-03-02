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
package dk.dtu.compute.cdl;

import org.javatuples.*;


public class Action {
  private final Pair<Integer, Integer> origin;
  private final Pair<Integer, Integer> destination;
  private final int time;
  private final String name;

  public Action(Pair<Integer, Integer> origin, Pair<Integer, Integer> destination, int time,
      String name) {
    this.origin = origin;
    this.destination = destination;
    this.time = time;
    this.name = name;
  }

  public Object get(String key) {
    switch (key) {
      case "dest":
      case "destination":
        return this.destination;
      case "orig":
      case "origin":
        return this.origin;
      case "time":
        return this.time;
      case "name":
        return this.name;
      case "edge":
        return new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(this.origin,
            this.destination);
      default:
        throw new IllegalArgumentException();
    }
  }
}
