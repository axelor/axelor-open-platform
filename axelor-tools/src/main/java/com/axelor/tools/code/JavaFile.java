/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * This class can be used to generate java source files for {@link JavaFile} in some given {@link
 * JavaContext}.
 */
public class JavaFile {

  private JavaType javaType;

  private JavaContext javaContext;

  /**
   * Create a new instance of the {@link JavaFile} with the given context and type.
   *
   * @param javaContext the context
   * @param javaType the type
   */
  public JavaFile(JavaContext javaContext, JavaType javaType) {
    this.javaContext = javaContext;
    this.javaType = javaType;
  }

  /**
   * Create a new instance of the {@link JavaFile} with the given package and type.
   *
   * @param packageName the package name
   * @param javaType the type
   */
  public JavaFile(String packageName, JavaType javaType) {
    this(new JavaContext(packageName), javaType);
  }

  /**
   * Get file path for the given base path.
   *
   * @param base the base directory from where to compute the path
   * @return the file path
   */
  public Path getPath(Path base) {
    String[] dirs = javaContext.getPackageName().split("\\.");
    String name = "%s.java".formatted(javaType.getName());
    return Stream.of(dirs).map(Paths::get).reduce(base, Path::resolve).resolve(name);
  }

  /**
   * Write the code to the given output.
   *
   * @param output the output target
   * @throws IOException if there is any error while writing to the output
   */
  public void writeTo(Appendable output) throws IOException {
    writeTo(output, s -> s);
  }

  /**
   * Write the code the given output by applying a custom formatter.
   *
   * @param output the output target
   * @param formatter the formatter
   * @throws IOException if there is any error while writing to the output
   */
  public void writeTo(Appendable output, Function<String, String> formatter) throws IOException {
    JavaWriter writer = new JavaWriter(javaContext, "  ").emit(javaType);
    JavaWriter header = new JavaWriter(javaContext, " ").emit(javaContext);
    output.append(formatter.apply(header.toString() + writer.toString()));
  }
}
