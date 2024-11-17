/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.util.annotations;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * @author douglm
 *
 */
public class ProcessState {
  private final ProcessingEnvironment env;

  String currentClassName;

  String resourcePath;

  /* Calculated size of fixed fields. */
  protected int sizeOverhead;

  boolean debug;

  public ProcessState(final ProcessingEnvironment env) {
    this.env = env;
  }

  public ProcessingEnvironment getEnv() {
    return env;
  }

  /**
   * @param val name of class
   */
  public void setCurrentClassName(final String val) {
    currentClassName = val;
  }

  /**
   * @return String
   */
  public String getCurrentClassName() {
    return currentClassName;
  }

  /**
   * @param tm TypeMirror
   * @return boolean
   */
  public static boolean isCollection(final TypeMirror tm) {
    if (!(tm instanceof DeclaredType)) {
      return false;
    }

    /* XXX There must be a better way than this */
    final String typeStr = tm.toString();

    return typeStr.startsWith("java.util.Collection") ||
           typeStr.startsWith("java.util.List") ||
           typeStr.startsWith("java.util.Set");
  }
}
