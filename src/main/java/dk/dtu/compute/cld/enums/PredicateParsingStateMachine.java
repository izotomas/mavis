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
package dk.dtu.compute.cld.enums;

public enum PredicateParsingStateMachine {
  OPERAND1 {
    @Override
    public PredicateParsingStateMachine next() {
      return OPERATOR;
    }
  },
  OPERATOR {
    @Override
    public PredicateParsingStateMachine next() {
      return OPERAND2;
    }
  },
  OPERAND2 {
    @Override
    public PredicateParsingStateMachine next() {
      return CONNECTOR;
    }
  },
  CONNECTOR {
    @Override
    public PredicateParsingStateMachine next() {
      return OPERAND1;
    }
  };

  public static PredicateParsingStateMachine initialize() {
    return OPERAND1;
  }

  public boolean isEndingState() {
    return this == CONNECTOR || this == OPERAND1;
  }

  public abstract PredicateParsingStateMachine next();
}
