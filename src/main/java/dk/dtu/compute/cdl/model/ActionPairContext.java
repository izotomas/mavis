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
import java.util.HashMap;
import org.javatuples.Pair;


public class ActionPairContext implements ValidationContext {

  private final HashMap<String, Object> map;

  public ActionPairContext(SimpleEntry<String, Action> entry1, SimpleEntry<String, Action> entry2) {
    this.map = new HashMap<>();

    this.mapContext(entry1);
    this.mapContext(entry2);
  }

  public void mapContext(SimpleEntry<String, Action> entry) {
    var actionRef = entry.getKey();
    var action = entry.getValue();

    this.map.put(actionRef, action);
    this.map.put(String.format("%s.dest", actionRef), action.destination);
    this.map.put(String.format("%s.destination", actionRef), action.destination);
    this.map.put(String.format("%s.orig", actionRef), action.origin);
    this.map.put(String.format("%s.origin", actionRef), action.origin);
    this.map.put(String.format("%s.time", actionRef), action.time);
    this.map.put(String.format("%s.name", actionRef), action.name);
    this.map.put(String.format("%s.edge", actionRef),
        new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(action.origin,
            action.destination));
  }

  public Object get(String key) {
    if (this.map.containsKey(key)) {
      return this.map.get(key);
    }
    throw new IllegalArgumentException();
  }
}
