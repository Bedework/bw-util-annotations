package org.bedework.util.annotations;

import java.io.Closeable;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

public class Template implements Closeable {
  private final ClassHandler cw;

  private LineNumberReader templateRdr;

  /** We use a template file which has code insertion points marked by lines
   * starting with "++++".
   *
   * @param cw for writing
   * @param templateName of file
   */
  public Template(final ClassHandler cw,
                     final String templateName) {
    this.cw = cw;

    try {
      templateRdr = new LineNumberReader(new FileReader(templateName));
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /** Close reader
   *
   */
  public void close() {
    try {
      if (templateRdr != null) {
        templateRdr.close();
      }
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /** Emit a section of template up to a delimiter or to end of file.
   *
   * @return true if read delimiter, false for eof.
   * @throws RuntimeException on error
   */
  public boolean emitSection() {
    for (;;) {
      final String ln;
      try {
        ln = templateRdr.readLine();
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }

      if (ln == null) {
        return false;
      }

      if (ln.startsWith("++++")) {
        return true;
      }

      cw.println(ln);
    }
  }
}
