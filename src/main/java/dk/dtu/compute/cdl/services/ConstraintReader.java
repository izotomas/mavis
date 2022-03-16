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
package dk.dtu.compute.cdl.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import dk.dtu.compute.cdl.errors.StatementParsingException;
import dk.dtu.compute.mavis.domain.ParseException;

public class ConstraintReader {
  private static final String hospitalConstraintsResourcePath = "/hospital.cld";
  private static final ConstraintParser parser = new ConstraintParser();

  public static List<ConstraintBuilder> read() throws IOException, ParseException {
    var url = ConstraintParser.class.getResource(hospitalConstraintsResourcePath);
    var in = new BufferedReader(new InputStreamReader(url.openStream()));

    var constraints = new ArrayList<ConstraintBuilder>();
    String inputLine;
    while ((inputLine = in.readLine()) != null) {
      try {
        var constraint = parser.Parse(inputLine);
        constraints.add(constraint);
      } catch (StatementParsingException e) {
        throw new ParseException(e.getMessage());
      }
    }
    return constraints;
  }

}
