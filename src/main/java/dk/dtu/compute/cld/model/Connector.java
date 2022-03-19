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

import java.util.regex.Pattern;
import dk.dtu.compute.cld.enums.ConnectorType;

public class Connector {
  private final static Pattern connectorPattern =
      Pattern.compile("^(?<type>AND|OR)(?<negated>\\hNOT)?$");

  public final ConnectorType type;
  public final boolean negated;

  public Connector(String connectorString) {
    var match = connectorPattern.matcher(connectorString);
    if (!match.find()) {
      throw new IllegalArgumentException(
          String.format("Invalid connector string: %s", connectorString));
    }
    this.type = ConnectorType.fromString(match.group("type"));
    this.negated = match.group("negated") != null;
  }

  protected Connector(ConnectorType type, boolean negated) {
    this.type = type;
    this.negated = negated;
  }
}
