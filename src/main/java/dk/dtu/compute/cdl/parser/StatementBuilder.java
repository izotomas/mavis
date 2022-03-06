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
package dk.dtu.compute.cdl.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import dk.dtu.compute.cdl.model.Action;

public class StatementBuilder {
  private final Set<String> ALLOWED_CONTEXT_KEYS = Set.of("entry1", "entry2");
  protected final HashMap<String, String> contextMap;

  protected Action contextEntry1;
  protected Action contextEntry2;

  public StatementBuilder() {
    this.contextMap = new HashMap<>();
  }

  public StatementBuilder withPrimaryActionContext(Action action) {
    if (action == null) {
      throw new IllegalArgumentException("Action context may not be null.");
    }
    this.contextEntry1 = action;
    return this;
  }

  public StatementBuilder withSecondaryActionContext(Action action) {
    if (action == null) {
      throw new IllegalArgumentException("Action context may not be null.");
    }
    this.contextEntry2 = action;
    return this;
  }

  protected StatementBuilder withContextMapping(SimpleEntry<String, String> entry)
      throws IllegalArgumentException {
    var key = entry.getKey();
    var val = entry.getValue();
    if (key.isEmpty() || val.isEmpty() || this.contextMap.containsKey(key)) {
      throw new IllegalArgumentException(String
          .format("Context mapping must be unique and non null.\n\tKey: %s | Value: %s", key, val));
    }
    if (!ALLOWED_CONTEXT_KEYS.contains(key)) {
      throw new IllegalArgumentException(String.format("Invalid key %s.\n\tAllowed keys: %s", key,
          Arrays.toString(ALLOWED_CONTEXT_KEYS.toArray())));
    }
    this.contextMap.put(key, val);
    return this;
  }
}
