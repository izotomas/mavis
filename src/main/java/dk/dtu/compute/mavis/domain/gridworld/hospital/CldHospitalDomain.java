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
package dk.dtu.compute.mavis.domain.gridworld.hospital;

import java.io.IOException;
import java.nio.file.Path;
import dk.dtu.compute.cdl.services.ConstraintReader;
import dk.dtu.compute.mavis.domain.ParseException;

/**
 * Limited Hospital domain (no boxes), using cld validation (hardcoded cld file)
 */
public class CldHospitalDomain extends HospitalDomain {
  public CldHospitalDomain(Path domainFile, boolean isLogFile) throws IOException, ParseException {
    super(domainFile, isLogFile, new CldValidator(new LevelReader(domainFile, isLogFile).getLevel(),
        ConstraintReader.read()));
  }
}
