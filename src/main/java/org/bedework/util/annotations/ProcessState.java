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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 * @author douglm
 *
 */
public abstract class ProcessState {
  private final ProcessingEnvironment env;

  private ClassHandler classHandler;

  /* Don't process inner classes - depth 0 is no class, depth 1 is outer class */
  private int classDepth;

  String resourcePath;

  /* Calculated size of fixed fields. */
  protected int sizeOverhead;

  private boolean debug;

  /** Should create new visitor on each call.
   *
   * @return visitor
   */
  public abstract ElementVisitor getVisitor();

  public ProcessState(final ProcessingEnvironment env) {
    this.env = env;
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

  public ClassHandler getClassHandler(final TypeMirror tm,
                                      final String outFileName) {
    classHandler = new ClassHandler(this,
                                    tm,
                                    outFileName);

    return classHandler;
  }

  public ClassHandler getClassHandler() {
    return classHandler;
  }

  public void closeClassHandler() {
    if (classHandler != null) {
      classHandler.close();
      classHandler = null;
    }
  }

  public void processClass(final Element el) {
    final String className = el.asType().toString();

    if (debug()) {
      note("Processing " + className);
    }

    el.accept(getVisitor(), this);
  }

  /** Override to do processing for a class
   *
   * @return true to process the class - false to skip.
   */
  public boolean startClass(final TypeElement el) {
    return true;
  }

  public void endClass(final TypeElement el) {
  }

  public void processMethod(final ExecutableElement el) {
  }

  /**
   *
   * @param tm for super class
   * @return true if we want super class processed as a separate class
   */
  public boolean shouldProcessSuperClass(final TypeMirror tm) {
    /* Something like
      return tm.toString().startsWith("org.bedework")
     */
    return false;
  }

  /**
   *
   * @param tm for super class
   * @return true if we want super methods embedded in current class
   */
  public boolean shouldProcessSuperMethods(final TypeMirror tm) {
    /* Something like
      return tm.toString().startsWith("org.bedework")
     */
    return false;
  }

  /**
   *
   * @param tm for super class
   */
  public void processSuperMethods(final TypeMirror tm) {
    final var el = env.getTypeUtils().asElement(tm);

    if (debug()) {
      note("process super method: " + el.toString());
    }

    for (final Element subEl: el.getEnclosedElements()) {
      if (subEl.getKind() == ElementKind.METHOD) {
        processMethod((ExecutableElement)subEl);
      }
    }

    final var typeEl =
            (TypeElement)env.getTypeUtils().asElement(el.asType());

    final TypeMirror superD = typeEl.getSuperclass();
    if (shouldProcessSuperMethods(superD)) {
      processSuperMethods(superD);
    }
  }

  public void processExecutable(final ExecutableElement e) {
    processMethod(e);
  }

  public void processingOver() {
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

  public void dumpElement(final String prefix,
                          final TypeElement el) {
    note(prefix + "Type parameters:");
    for (final var tp: el.getTypeParameters()) {
      dumpElement(prefix, tp);
    }
    dumpElement(prefix, (Element)el);
  }

  public void dumpElement(final String prefix,
                          final ExecutableElement el) {
    dumpElement(prefix, (Element)el);

    note(prefix + "Type parameters:");
    for (final var tp: el.getTypeParameters()) {
      dumpElement(prefix + "  ", tp);
    }
    note(prefix + "Parameters:");
    for (final var p: el.getParameters()) {
      dumpElement(prefix + "  ", p);
    }
  }

  public void dumpElement(final String prefix,
                          final VariableElement el) {
    note(prefix + " param: " + el.getSimpleName());
    note(prefix + " const: " + el.getConstantValue());
    note(prefix + " type: " + el.asType());

    final var typEl = (TypeElement)env.getTypeUtils()
                                      .asElement(el.asType());
    if (typEl != null) {
      dumpElement(prefix + "  ", typEl);
    }
    /*
    dumpElement(prefix,
                (TypeElement)env.getTypeUtils()
                                .asElement(el.asType()));
     */
  }

  public void dumpElement(final String prefix,
                          final TypeParameterElement el) {
    note(prefix + "  generic: " + el.getGenericElement());
    dumpElement(prefix + "  ", (Element)el);
  }

  public void dumpElement(final String prefix,
                          final Element el) {
    note(prefix + " name:" + el.getSimpleName());
    note(prefix + " kind:" + el.getKind());
    note(prefix + "class:" + el.toString());
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
