/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.text;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.groovy.util.SystemUtil;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

/**
 * GStringTemplateEngine implementation that allows to pass CompilerConfiguration.
 *
 * <p>This allows to apply custom script transformation (for scripting policy).
 *
 * <p>Also keep default of not reusing class loader so that each script class can individually be
 * garbage collected.
 */
public class AxelorGStringTemplateEngine extends GStringTemplateEngine {

  protected final ClassLoader parentLoader;
  protected final CompilerConfiguration config;
  protected static AtomicInteger counter = new AtomicInteger();
  protected static final boolean REUSE_CLASS_LOADER =
      SystemUtil.getBooleanSafe("groovy.GStringTemplateEngine.reuseClassLoader");

  /**
   * Creates a GStringTemplateEngine with the given parent class loader and compiler configuration.
   *
   * @param parentLoader
   * @param config
   */
  public AxelorGStringTemplateEngine(ClassLoader parentLoader, CompilerConfiguration config) {
    super(parentLoader);
    this.parentLoader = parentLoader;
    this.config = config;
  }

  /* (non-Javadoc)
   * @see groovy.text.TemplateEngine#createTemplate(java.io.Reader)
   */
  @Override
  public Template createTemplate(final Reader reader)
      throws CompilationFailedException, ClassNotFoundException, IOException {
    return new GStringTemplate(reader, parentLoader, config);
  }

  private static class GStringTemplate implements Template {
    final Closure template;

    /**
     * Turn the template into a writable Closure When executed the closure evaluates all the code
     * embedded in the template and then writes a GString containing the fixed and variable items to
     * the writer passed as a parameter
     *
     * <p>For example:
     *
     * <pre>
     * '<%= "test" %> of expr and <% test = 1 %>${test} script.'
     * </pre>
     *
     * <p>would compile into:
     *
     * <pre>
     * { out -> out << "${"test"} of expr and "; test = 1 ; out << "${test} script."}.asWritable()
     * </pre>
     *
     * @param reader
     * @param parentLoader
     * @param config
     * @throws CompilationFailedException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    GStringTemplate(
        final Reader reader, final ClassLoader parentLoader, final CompilerConfiguration config)
        throws CompilationFailedException, ClassNotFoundException, IOException {
      final StringBuilder templateExpressions =
          new StringBuilder(
              "package groovy.tmp.templates\n def getTemplate() { return { out -> out << \"\"\"");
      boolean writingString = true;

      while (true) {
        int c = reader.read();
        if (c == -1) break;
        if (c == '<') {
          c = reader.read();
          if (c == '%') {
            c = reader.read();
            if (c == '=') {
              parseExpression(reader, writingString, templateExpressions);
              writingString = true;
              continue;
            } else {
              parseSection(c, reader, writingString, templateExpressions);
              writingString = false;
              continue;
            }
          } else {
            appendCharacter('<', templateExpressions, writingString);
            writingString = true;
          }
        } else if (c == '"') {
          appendCharacter('\\', templateExpressions, writingString);
          writingString = true;
        } else if (c == '$') {
          appendCharacter('$', templateExpressions, writingString);
          writingString = true;
          c = reader.read();
          if (c == '{') {
            appendCharacter('{', templateExpressions, writingString);
            writingString = true;
            parseGSstring(reader, writingString, templateExpressions);
            writingString = true;
            continue;
          }
        }
        appendCharacter((char) c, templateExpressions, writingString);
        writingString = true;
      }

      if (writingString) {
        templateExpressions.append("\"\"\"");
      }

      templateExpressions.append("}}");

      // Use a new class loader by default for each class so each class can be independently garbage
      // collected
      final GroovyClassLoader loader =
          REUSE_CLASS_LOADER && parentLoader instanceof GroovyClassLoader
              ? (GroovyClassLoader) parentLoader
              : createClassLoader(parentLoader, config);
      final Class<?> groovyClass;
      try {
        groovyClass =
            loader.parseClass(
                new GroovyCodeSource(
                    templateExpressions.toString(),
                    "GStringTemplateScript" + counter.incrementAndGet() + ".groovy",
                    "x"));
      } catch (Exception e) {
        throw new GroovyRuntimeException(
            "Failed to parse template script (your template may contain an error or be trying to"
                + " use expressions not currently supported): "
                + e.getMessage());
      }

      try {
        final GroovyObject script =
            (GroovyObject) groovyClass.getDeclaredConstructor().newInstance();

        this.template = (Closure) script.invokeMethod("getTemplate", null);
        // GROOVY-6521: must set strategy to DELEGATE_FIRST, otherwise writing
        // books = 'foo' in a template would store 'books' in the binding of the template script
        // itself ("script")
        // instead of storing it in the delegate, which is a Binding too
        this.template.setResolveStrategy(Closure.DELEGATE_FIRST);
      } catch (InstantiationException
          | IllegalAccessException
          | NoSuchMethodException
          | InvocationTargetException e) {
        throw new ClassNotFoundException(e.getMessage());
      }
    }

    @SuppressWarnings(
        "removal") // TODO a future Groovy version should create the loader not as a privileged
    // action
    private GroovyClassLoader createClassLoader(
        ClassLoader parentLoader, CompilerConfiguration config) {
      return java.security.AccessController.doPrivileged(
          (PrivilegedAction<GroovyClassLoader>) () -> new GroovyClassLoader(parentLoader, config));
    }

    private static void appendCharacter(
        final char c, final StringBuilder templateExpressions, final boolean writingString) {
      if (!writingString) {
        templateExpressions.append("out << \"\"\"");
      }
      templateExpressions.append(c);
    }

    private void parseGSstring(
        Reader reader, boolean writingString, StringBuilder templateExpressions)
        throws IOException {
      if (!writingString) {
        templateExpressions.append("\"\"\"; ");
      }
      while (true) {
        int c = reader.read();
        if (c == -1) break;
        templateExpressions.append((char) c);
        if (c == '}') {
          break;
        }
      }
    }

    /**
     * Parse a &lt;% .... %&gt; section if we are writing a GString close and append ';' then write
     * the section as a statement
     *
     * @param pendingC
     * @param reader
     * @param writingString
     * @param templateExpressions
     * @throws IOException
     */
    private static void parseSection(
        final int pendingC,
        final Reader reader,
        final boolean writingString,
        final StringBuilder templateExpressions)
        throws IOException {
      if (writingString) {
        templateExpressions.append("\"\"\"; ");
      }
      templateExpressions.append((char) pendingC);

      readAndAppend(reader, templateExpressions);

      templateExpressions.append(";\n ");
    }

    private static void readAndAppend(Reader reader, StringBuilder templateExpressions)
        throws IOException {
      while (true) {
        int c = reader.read();
        if (c == -1) break;
        if (c == '%') {
          c = reader.read();
          if (c == '>') break;
          templateExpressions.append('%');
        }
        templateExpressions.append((char) c);
      }
    }

    /**
     * Parse a &lt;%= .... %&gt; expression
     *
     * @param reader
     * @param writingString
     * @param templateExpressions
     * @throws IOException
     */
    private static void parseExpression(
        final Reader reader, final boolean writingString, final StringBuilder templateExpressions)
        throws IOException {
      if (!writingString) {
        templateExpressions.append("out << \"\"\"");
      }

      templateExpressions.append("${");

      readAndAppend(reader, templateExpressions);

      templateExpressions.append('}');
    }

    @Override
    public Writable make() {
      return make(null);
    }

    @Override
    public Writable make(final Map map) {
      final Closure templateClosure = ((Closure) this.template.clone()).asWritable();
      Binding binding = new Binding(map);
      templateClosure.setDelegate(binding);
      return (Writable) templateClosure;
    }
  }
}
