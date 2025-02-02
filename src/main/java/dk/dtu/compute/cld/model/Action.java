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

import org.javatuples.*;

public class Action {
  /**
   * origin coordinates (x,y)
   */
  public final Pair<Integer, Integer> origin;
  /**
   * destination coordinates (x,y)
   */
  public final Pair<Integer, Integer> destination;

  /**
   * timestep
   */
  public final Integer time;

  /**
   * action name
   */
  public final String name;

  /**
   * agent (id) performing the action
   */
  public final Integer agent;

  /**
   * Action model used as context for constraint validation
   * 
   * @param origin orgin coodinates
   * @param destination detination coordinates
   * @param time time when action is being executed/requested
   * @param name name of the action
   * @param agent agent id attempting the action
   */
  public Action(Pair<Integer, Integer> origin, Pair<Integer, Integer> destination, Integer time,
      String name, Integer agent) {
    this.origin = origin;
    this.destination = destination;
    this.time = time;
    this.name = name;
    this.agent = agent;
  }

  /**
   * Constructor only for testing purpose
   * 
   * @param origX origin X coordinate
   * @param origY origin Y coordinate
   * @param destX destination X coordinate
   * @param destY destination Y coordinate
   */
  protected Action(int origX, int origY, int destX, int destY) {
    this.origin = new Pair<>(origX, origY);
    this.destination = new Pair<>(destX, destY);
    this.time = 0;
    this.name = null;
    this.agent = 0;
  }
}
