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
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 * @author douglm
 *
 */
public class ProcessState {
  private final ProcessingEnvironment env;

  private final UtilAnn util;

  String currentClassName;

  /* Don't process inner classes - depth 0 is no class, depth 1 is outer class */
  private int classDepth;

  String resourcePath;

  /* Calculated size of fixed fields. */
  protected int sizeOverhead;

  private boolean debug;

  public ProcessState(final ProcessingEnvironment env) {
    this.env = env;
    util = new UtilAnn(env);
  }

  public ProcessingEnvironment env() {
    return env;
  }

  /** Override to process any custom options
   *
   * @param name of option
   * @param value of option
   */
  public void option(final String name, final String value) {}

  /** Override to do processing for a class
   *
   * @return true to process the class - false to skip.
   */
  public boolean startClass(final TypeElement el) {
    return true;
  }

  public void endClass(final TypeElement el) {
  }

  public void processMethod(final Element el) {
  }

  public boolean shouldProcessSuper(final TypeMirror tm) {
    /* Something like
      return tm.toString().startsWith("org.bedework")
     */
    return false;
  }

  public void processSuper(final TypeMirror tm) {
    final var el = env.getTypeUtils().asElement(tm);

    if (debug()) {
      note("process super: " + el.toString());
    }

    for (final Element subEl: el.getEnclosedElements()) {
      if (subEl.getKind() == ElementKind.METHOD) {
        processMethod(subEl);
      }
    }

    final var typeEl =
            (TypeElement)env.getTypeUtils().asElement(el.asType());

    final TypeMirror superD = typeEl.getSuperclass();
    if (shouldProcessSuper(superD)) {
      processSuper(superD);
    }
  }

  public void processExecutable(final ExecutableElement e) {
  }

  public void processingOver() {
  }

  public UtilAnn util() {
    return util;
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

  public boolean debug() {
    return debug;
  }

  public void setDebug(final boolean val) {
    debug = val;
  }

  public String getResourcePath() {
    return resourcePath;
  }

  public void setResourcePath(final String val) {
    resourcePath = val;
  }

  public void incClassDepth() {
    classDepth++;
  }

  public void decClassDepth() {
    classDepth--;
  }

  public int classDepth() {
    return classDepth;
  }

  public void error(final String msg) {
    env.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
  }

  public void warn(final String msg) {
    env.getMessager().printMessage(Diagnostic.Kind.WARNING, msg);
  }

  public void note(final String msg) {
    // Maven swallowing output
    System.out.println(msg);
    env.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
  }
}
