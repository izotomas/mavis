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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import dk.dtu.compute.cdl.enums.OperandType;
import dk.dtu.compute.cdl.enums.OperandValueType;

public class Operand {
  private final static Pattern numberPattern = Pattern.compile("^\\d+$");
  private final static Pattern stringPattern = Pattern.compile("^\\'(?<value>[\\w\\h().,*]+)\\'$");
  private final static Pattern actionPropPattern = Pattern.compile(
      "^(?<actionkey>[a-z]+)\\.((?<orig>orig(?:in)?)|(?<dest>dest(?:ination)?)|(?<edge>edge)|(?<time>time)|(?<name>name))$");

  protected final Object value;

  public final OperandValueType valueType;
  public final OperandType type;
  public final String actionKey;

  public Operand(String valueString) {
    Matcher matcher;
    if ((matcher = stringPattern.matcher(valueString)).matches()) {
      this.valueType = OperandValueType.String;
      this.value = matcher.group("value");
      this.type = OperandType.Literal;
      this.actionKey = null;
    } else if ((matcher = numberPattern.matcher(valueString)).matches()) {
      this.valueType = OperandValueType.Number;
      this.value = (Integer) Integer.parseInt(valueString);
      this.type = OperandType.Literal;
      this.actionKey = null;
    } else if ((matcher = actionPropPattern.matcher(valueString)).matches()) {
      this.value = valueString;
      this.type = OperandType.ActionReference;
      this.actionKey = matcher.group("actionkey");
      if (matcher.group("orig") != null) {
        this.valueType = OperandValueType.Vertex;
      } else if (matcher.group("dest") != null) {
        this.valueType = OperandValueType.Vertex;
      } else if (matcher.group("edge") != null) {
        this.valueType = OperandValueType.Edge;
      } else if (matcher.group("time") != null) {
        this.valueType = OperandValueType.Number;
      } else if (matcher.group("name") != null) {
        this.valueType = OperandValueType.String;
      } else {
        throw new IllegalArgumentException(String.format("Not a valid operand: %s", valueString));
      }
    } else {
      throw new IllegalArgumentException(String.format("Not a valid operand: %s", valueString));
    }
  }

  public boolean compatibleWith(Operand other) {
    return this.valueType == other.valueType;
  }

  public Object getValue(ValidationContext context) {
    return this.type == OperandType.Literal ? this.value : context.get((String) this.value);
  }
}
