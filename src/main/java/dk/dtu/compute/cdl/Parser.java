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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.awt.Color;
import sallat.parser.*;

public class Parser {

  private final static String contextPredicateSplitPattern = "\\sIF\\s";
  private final static String contextEntriesPattern =
      "^ACTION\\s(?<entry1>[a-z]+)\\sIS\\sBLOCKED(\\s(?<BY>BY)\\sACTION\\s(?<entry2>[a-z]+))?$";

  private final static Pattern predicatePattern = Pattern.compile(
      // start
      "^(?:"
          // predicate connector
          + "(?:\\h(?:(?:AND|OR)(?:\\hNOT)?)\\h|^)"
          // operand1
          + "(?:[a-z]+\\.[a-z]+|\\'[\\w().,*]+\\'|[0-9]+)"
          // operator
          + "\\h(?:[A-Z]+(?:\\h[A-Z]+)*)\\h"
          // operand2
          + "(?:[a-z]+\\.[a-z]+|\\'[\\w().,*]+\\'|[0-9]+)"
          // repeat
          + ")+$");


  public void Parse(String cldStatement) {
    // 1. split into context and conditions
    // 2. parse context into context builder
    // 3.

  }


  public static Predicate<Cat> shouldNotFail() {
    Map<String, Operators> operatorsMap = new HashMap<>();

    operatorsMap.put("not", Operators.NOT);
    operatorsMap.put("and", Operators.AND);
    operatorsMap.put("or", Operators.OR);
    operatorsMap.put("xor", Operators.XOR);

    // define mappings for the elementary predicates

    Map<String, Predicate<Cat>> predicateMap = new HashMap<>();

    predicateMap.put("fluffy", Cat::isFluffy);
    predicateMap.put("black", cat -> cat.getColor() == Color.BLACK);
    predicateMap.put("white", cat -> cat.getColor() == Color.WHITE);
    predicateMap.put("hungry", Cat::isHungry);
    predicateMap.put("tom", cat -> cat.equals("tom"));

    // Build a PredicateParser instance

    PredicateParser<Cat> parser =
        SimplePredicateParser.<Cat>builder().setCasePolicy(CasePolicy.TO_LOWER_CASE)
            .setOperatorMap(operatorsMap).setPredicateMap(predicateMap).build();

    // Use the parser to create predicates

    var predicate = parser.parse("(fluffy or hungry) and not (black or white or tom)");
    return predicate;
  }

}
