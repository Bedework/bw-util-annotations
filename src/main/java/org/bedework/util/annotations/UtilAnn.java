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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/** Utility methods for annotations
 *
 * @author Mike DOuglass
 */
public class UtilAnn {
  private final ProcessingEnvironment env;

  private String className;

  /** Initialise to handle messages
   *
   * @param env the processing environment
   */
  public UtilAnn(final ProcessingEnvironment env) {
    this.env = env;
  }

  /**
   * @param tm for possible collection
   * @return boolean
   */
  public boolean isCollection(final TypeMirror tm) {
    final var el = (TypeElement)env.getTypeUtils().asElement(tm);

    if (el == null) {
      return false;
    }

    if (el.getKind() == ElementKind.CLASS) {
      for (final TypeMirror itm: el.getInterfaces()) {
        if (testCollection(itm)) {
          return  true;
        }
      }

      return false;
    }

    return testCollection(tm);
  }

  /**
   * @param tm TypeMirror
   * @return boolean
   */
  public static boolean testCollection(final TypeMirror tm) {
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
