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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

import static java.lang.String.format;

/** Utility methods for annotations
 *
 * @author Mike DOuglass
 */
public class ClassWriter {
  private final String className;

  private final PrintWriter out;

  /**
   * @param env the processing environment
   * @param className we're processing
   * @param outFileName for generated file.
   */
  public ClassWriter(final ProcessingEnvironment env,
                     final String className,
                     final String outFileName) {
    this.className = className;
    try {
      final JavaFileObject outFile =
              env.getFiler().createSourceFile(outFileName);
      out = new PrintWriter(outFile.openOutputStream());
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /** Close readers/writers
   *
   * @throws IOException on error
   */
  public void close() throws IOException {
    if (out != null) {
      out.close();
    }
  }

  /** Generate a call to a getter.
   *
   * @param objRef - the reference to the getters class
   * @param ucFieldName - name of field with first char upper cased
   * @return String call to getter
   */
  public static String makeCallGetter(final String objRef,
                                      final String ucFieldName) {
    final StringBuilder sb = new StringBuilder(objRef);
    sb.append(".");
    sb.append("get");
    sb.append(ucFieldName);
    sb.append("()");

    return sb.toString();
  }

  /** Make a call to a setter
   *
   * @param objRef - the reference to the getters class
   * @param ucFieldName - name of field wit first char upper cased
   * @param val - represents the value
   * @return String call to setter
   */
  public static String makeCallSetter(final String objRef,
                                      final String ucFieldName,
                                      final Object val) {
    return format(".set%s(%s)", ucFieldName, val);
  }

  /**
   * @param methName
   * @param pars
   * @param returnType
   * @param thrownTypes
   */
  public void generateSignature(final String methName,
                                final List<? extends VariableElement> pars,
                                final TypeMirror returnType,
                                final List<? extends TypeMirror> thrownTypes) {
    final String rType = getClassName(returnType);

    final var part1 = format("  public %s %s(", rType, methName);
    out.print(part1);
    final var pad = " ".repeat(part1.length());

    var i = 0;

    for (final VariableElement par: pars) {
      prntncc(fixName(par.asType().toString()), " ",
              par.getSimpleName().toString());

      i++;
      if (i < pars.size()) {
        out.println(", ");
        out.print(pad);
      }
    }

    out.print(")");

    if (!thrownTypes.isEmpty()) {
      out.println();
      out.print("        throws ");
      String delim = "";
      for (final TypeMirror rt: thrownTypes) {
        out.print(delim);
        delim = ", ";

        out.print(fixName(rt.toString()));
      }
    }

    out.println(" {");
  }

  /**
   * @param tm TypeMirror for class
   * @return String
   */
  public String getClassName(final TypeMirror tm) {
    return fixName(tm.toString());
  }

  /** Return a name we might need to import or null.
   *
   * @param tm TypeMirror for class
   * @param thisPackage current package name
   * @return String
   */
  public static String getImportableClassName(
          final TypeMirror tm,
          final String thisPackage) {
    if (tm.getKind() == TypeKind.VOID) {
      return null;
    }

    if (!tm.getKind().isPrimitive()) {
      final String className = nonGeneric(tm.toString());

      if (className.startsWith("java.lang.")) {
        return null;
      }

      if (samePackage(thisPackage, className)) {
        return null;
      }

      return className;
    }

    return null;
  }

  /**
   * @param str name to fix
   * @return String
   */
  public static String fixName(String str) {
    if (str == null) {
      return null;
    }

    /* Has to be  a better way than this */

    str = str.replaceAll("java\\.util\\.", "")
             .replaceAll("java\\.lang\\.", "");
    /*
    str = str.replaceAll("org\\.bedework\\.calfacade\\.Bw", "Bw");
    str = str.replaceAll("org\\.bedework\\.calfacade\\.base\\.Bw", "Bw");
    str = str.replaceAll("org\\.bedework\\.calfacade\\.wrappers\\.Bw", "Bw");
     */

    return str;
  }

  /**
   * @param packageName the package
   * @param importName non-null if import needed
   * @param simpleClassName name with package removed
   */
  public record SplitClassName(String packageName,
                               String importName,
                               String simpleClassName) {
  }

  public SplitClassName getSplitClassName(final TypeMirror tm) {
    final var className = nonGeneric(tm.toString());
    final int pos = className.lastIndexOf('.');
    if (pos < 0) {
      throw new IllegalArgumentException("Invalid class name: " +
                                                 className);
    }

    final var pkg = className.substring(0, pos);
    return new SplitClassName(pkg, getImportableClassName(tm, pkg),
                              className.substring(pos + 1));
  }

  /** Return the non-generic type (class without the type parameters) for the
   * given type string
   *
   * <p>Note: ClassDeclaration.getQualifiedName() does this.
   *
   * @param type
   * @return String generic type name
   */
  public static String nonGeneric(final String type) {
    if (!type.endsWith(">")) {
      return type;
    }

    final int pos = type.indexOf("<");
    return type.substring(0, pos);
  }

  /** ClassDeclaration.getPackage() could be useful here
   *
   * @param thisPackage our package name
   * @param thatClass fully qualified class
   * @return boolean
   */
  public static boolean samePackage(final String thisPackage,
                                    final String thatClass) {
    //env.getMessager().printNotice("samePackage: " + thisPackage + " " + thatClass);

    if (!thatClass.startsWith(thisPackage)) {
      return false;
    }

    if (thatClass.charAt(thisPackage.length()) != '.') {
      return false;
    }

    return thatClass.indexOf(".", thisPackage.length() + 1) < 0;
  }

  /**
   * @param val
   */
  public void prntncc(final String... val) {
    for (final String ln: val) {
      out.print(ln);
    }
  }

  /** print bunch of values on same line and end line
   *
   * @param val values
   */
  public void println(final String... val) {
    for (final String ln: val) {
      out.print(ln);
    }

    out.println();
  }

  /**
   * @param val a line to print
   */
  public void println(final String val) {
    out.println(val);
  }

  /**
   * @param lines to pront
   */
  public void prntlns(final String... lines) {
    for (final String ln: lines) {
      out.println(ln);
    }
  }
}
