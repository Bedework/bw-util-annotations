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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

import static java.lang.String.format;

/** Class for writing classes. We store it all in a bunch of
 * variables until close when we write it out. This allows us
 * to add imports as we process the class.
 *
 * @author Mike DOuglass
 */
public class ClassHandler {
  private final TypeMirror tm;
  private final String packageName;
  private final String outFileName;

  private final PrintWriter out;

  private String packageLine;
  private final TreeSet<String> imports = new TreeSet<>();
  private String classStart;
  private final TreeSet<String> fields = new TreeSet<>();
  private final List<String> constructors = new ArrayList<>();
  private final List<String> methods = new ArrayList<>();
  private String classEnd;

  private StringBuilder buf;

  /**
   * @param env the processing environment
   * @param tm for class we're processing
   * @param outFileName for generated file.
   */
  public ClassHandler(final ProcessingEnvironment env,
                      final TypeMirror tm,
                      final String outFileName) {
    this.tm = tm;
    packageName = getPackage(tm.toString());
    this.outFileName = outFileName;
    try {
      final JavaFileObject outFile =
              env.getFiler().createSourceFile(outFileName);
      out = new PrintWriter(outFile.openOutputStream());
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public void end() {
    if (packageLine == null) {
      return;
    }

    out.println(packageLine);
    for (final var imp: imports) {
      out.println(imp);
    }
    out.println();

    out.println(classStart);
    for (final var field: fields) {
      out.println(field);
    }
    out.println();
    for (final var constructor: constructors) {
      out.println(constructor);
    }
    out.println();
    for (final var method: methods) {
      out.println(method);
    }
    out.println("}");
  }

  /** Close readers/writers
   *
   */
  public void close() {
    if (out != null) {
      out.close();
    }
  }

  public void startPackage(final String name) {
    packageLine = format("package %s;", name);
  }

  public void addImport(final String name) {
    imports.add(format("import %s;", name));
  }

  public void generateClassStart() {
    final var split = getSplitClassName(tm);
    final var outClassName = getSimpleClassName(outFileName);

    startPackage(split.packageName);
    classStart = format("public class %s {",
                        outClassName);
    addField(format("  private final %s entity; ",
                    split.simpleClassName));
    constructors.add(
      format("""
                public %s(final %s entity) {
                  this.entity = entity;
                }
              """, outClassName, split.simpleClassName));
  }

  public void addField(final String def) {
    fields.add(def);
  }

  public void addMethod(final String def) {
    methods.add(def);
  }

  /**
   * @param methName
   * @param pars
   * @param returnType
   * @param thrownTypes
   */
  public String generateSignature(
          final String methName,
          final List<? extends VariableElement> pars,
          final TypeMirror returnType,
          final List<? extends TypeMirror> thrownTypes) {
    final var rsplit = getSplitClassName(returnType);

    if (rsplit.importName != null) {
      addImport(rsplit.importName);
    }

    final var part1 = format("  public %s %s(",
                             rsplit.simpleClassName,
                             methName);
    final var buf = new StringBuilder(part1);

    final var pad = " ".repeat(part1.length());

    var i = 0;

    for (final VariableElement par: pars) {
      final var psplit = getSplitClassName(par.asType());
      if (psplit.importName != null) {
        addImport(psplit.importName);
      }

      buf.append(format("%s %s",
                        psplit.simpleClassName,
                        par.getSimpleName().toString()));

      i++;
      if (i < pars.size()) {
        buf.append(", ");
        buf.append(pad);
      }
    }

    buf.append(")");

    if (!thrownTypes.isEmpty()) {
      buf.append("\n");
      buf.append("        throws ");
      String delim = "";
      for (final TypeMirror rt: thrownTypes) {
        buf.append(delim);
        delim = ", ";

        buf.append(fixName(rt.toString()));
      }
    }

    buf.append(" {\n");
    
    return buf.toString();
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
   * @param ucFieldName - name of field with first char upper cased
   * @param val - represents the value
   * @return String call to setter
   */
  public static String makeCallSetter(final String objRef,
                                      final String ucFieldName,
                                      final Object val) {
    return format(".set%s(%s)", ucFieldName, val);
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
   * @return String
   */
  public String getImportableClassName(
          final TypeMirror tm) {
    if (tm.getKind() == TypeKind.VOID) {
      return null;
    }

    if (!tm.getKind().isPrimitive()) {
      final String className = nonGeneric(tm.toString());

      if (className.startsWith("java.lang.")) {
        return null;
      }

      if (samePackage(className)) {
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

  public String getSimpleClassName(final String className) {
    final int pos = className.lastIndexOf('.');
    if (pos < 0) {
      throw new IllegalArgumentException("Invalid class name: " +
                                                 className);
    }

    return className.substring(pos + 1);
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
    if (tm.getKind().isPrimitive() || "void".equals(className)) {
      return new SplitClassName(null, null, className);
    }
    final int pos = className.lastIndexOf('.');
    if (pos < 0) {
      throw new IllegalArgumentException("Invalid class name: " +
                                                 className);
    }

    final var pkg = className.substring(0, pos);
    return new SplitClassName(pkg, getImportableClassName(tm),
                              className.substring(pos + 1));
  }

  /**
   *
   * @param className fully qualified name
   * @return package
   */
  public String getPackage(final String className) {
    final int pos = nonGeneric(className).lastIndexOf('.');
    if (pos < 0) {
      throw new IllegalArgumentException("Invalid class name: " +
                                                 className);
    }

    return className.substring(0, pos);
  }

  /**
   * @param setter true for set method
   * @param ucFieldName of associated field
   */
  public record SplitMethodName(boolean setter,
                                String fieldName,
                                String ucFieldName,
                                String methodName) {
  }

  public SplitMethodName getSplitMethodName(final ExecutableElement e) {
    final var name = e.getSimpleName().toString();

    if (!name.startsWith("get") && !name.startsWith("set")) {
      throw new IllegalArgumentException(
              "Invalid method for annotation: " + name);
    }

    final var ucFieldName = name.substring(3);
    if (!Character.isUpperCase(ucFieldName.charAt(0))) {
      throw new IllegalArgumentException(
              "Invalid method for annotation: " + name);
    }

    final var fieldName =
            ucFieldName.substring(0, 1).toLowerCase() +
            ucFieldName.substring(1);

    return new SplitMethodName(name.startsWith("set"),
                               fieldName,
                               ucFieldName,
                               name);
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
   * @param thatClass fully qualified class
   * @return boolean
   */
  public boolean samePackage(final String thatClass) {
    if (!thatClass.startsWith(packageName)) {
      return false;
    }

    if (thatClass.charAt(packageName.length()) != '.') {
      return false;
    }

    return thatClass.indexOf(".", packageName.length() + 1) < 0;
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
