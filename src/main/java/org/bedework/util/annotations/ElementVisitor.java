package org.bedework.util.annotations;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor14;

import static java.lang.String.format;

public class ElementVisitor extends
        SimpleElementVisitor14<Element, ProcessState> {
  @Override
  public Element visitType(final TypeElement el,
                           final ProcessState pstate) {
    final String className = el.asType().toString();

    if (pstate.debug()) {
      pstate.note("Start Class: " + className +
                          " depth: " + pstate.classDepth());
    }

    pstate.incClassDepth();

    if ((pstate.classDepth() <= 1) &&       // In inner class
            pstate.startClass(el)) {
      for (final Element subEl: el.getEnclosedElements()) {
        subEl.accept(new ElementVisitor(), pstate);
      }
    }

    /* Now we do the end processing */

    pstate.decClassDepth();

    if (pstate.debug()) {
      pstate.note(format("End Class: %s depth: %s",
                         className, pstate.classDepth()));
    }

    if (pstate.classDepth() >= 1) {
      // Finished inner class
      return el;
    }

    final TypeMirror superD = el.getSuperclass();
    if (pstate.shouldProcessSuperMethods(superD)) {
      pstate.processSuperMethods(superD);
    }

    pstate.endClass(el);

    if (pstate.shouldProcessSuperClass(superD)) {
      pstate.processClass(pstate.env()
                                .getTypeUtils()
                                .asElement(el.asType()));
    }

    return el;
  }

  @Override
  public Element visitExecutable(final ExecutableElement e,
                                 final ProcessState pstate) {
    if (pstate.classDepth() > 1) {
      // In inner class
      return e;
    }

    pstate.processExecutable(e);

    return e;
  }
}
