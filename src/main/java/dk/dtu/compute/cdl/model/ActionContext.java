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

import org.javatuples.Pair;

public class ActionContext implements ValidationContext {
  private final Action action;

  public ActionContext(Action action) {
    this.action = action;
  }

  public Object get(String key) {
    switch (key) {
      case "dest":
      case "destination":
        return this.action.destination;
      case "orig":
      case "origin":
        return this.action.origin;
      case "time":
        return this.action.time;
      case "name":
        return this.action.name;
      case "edge":
        return new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(this.action.origin,
            this.action.destination);
      default:
        throw new IllegalArgumentException();
    }
  }
}
