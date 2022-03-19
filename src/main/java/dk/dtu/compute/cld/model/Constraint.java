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
package dk.dtu.compute.cld.model;

import java.util.function.Predicate;

public class Constraint {
  public final ValidationContext context;
  public final Predicate<ValidationContext> predicate;

  public Constraint(ValidationContext context, Predicate<ValidationContext> predicate) {
    this.context = context;
    this.predicate = predicate;
  }

  public boolean evaluate() {
    return this.predicate.test(this.context);
  }
}
