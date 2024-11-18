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

import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import static java.lang.String.format;

/**
 * @author douglm
 *
 */
@SupportedAnnotationTypes(value= {"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public abstract class AnnotationProcessor
        extends AbstractProcessor {
  private ProcessState pstate;

  /** Should create new state only on first call.
   *
   * @param env the environment
   * @return new or existing state
   */
  public abstract ProcessState getState(ProcessingEnvironment env);

  /** Should create new visitor on each call.
   *
   * @return visitor
   */
  public abstract ElementVisitor getVisitor();

  @Override
  public void init(final ProcessingEnvironment env) {
    super.init(env);

    pstate = getState(processingEnv);

    final Map<String, String> options = env.getOptions();
    for (final String option: options.keySet()) {
      final var val = options.get(option);
      pstate.note(format("Option: %s=%s",
                         option, val));
      if (option.equals("resourcePath")) {
        pstate.setResourcePath(val);
        continue;
      }

      if (option.equals("debug")) {
        pstate.setDebug("true".equals(val));
        continue;
      }

      pstate.option(option, val);
    }
  }

  @Override
  public boolean process(final Set<? extends TypeElement> annotations,
                         final RoundEnvironment roundEnv) {
    if (pstate.debug()) {
      pstate.note(
              "--------------- process called: " + roundEnv
                      .toString());

      for (final TypeElement tel : annotations) {
        pstate.note("Annotation " + tel.asType().toString());
      }
    }

    for (final Element el: roundEnv.getRootElements()) {
      final String className = el.asType().toString();

      if (pstate.debug()) {
        pstate.note("Processing " + className);
      }

      el.accept(getVisitor(), pstate);
    }

    if (roundEnv.processingOver()) {
      pstate.processingOver();
    }

    return false;
  }
}