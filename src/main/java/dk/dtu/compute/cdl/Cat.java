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

import java.awt.Color;

public class Cat {
  boolean isFluffy;
  boolean isHungry;
  Color color;
  String name;

  public Cat() {}

  public Cat(boolean isFluffy, boolean isHungry, Color color, String name) {
    this.isFluffy = isFluffy;
    this.isHungry = isHungry;
    this.color = color;
    this.name = name;

  }

  boolean isFluffy() {
    return this.isFluffy;
  }

  boolean isHungry() {
    return this.isHungry;
  }

  Color getColor() {
    return this.color;
  }

  boolean equals(String name) {
    return this.name.equals(name);
  }
}
