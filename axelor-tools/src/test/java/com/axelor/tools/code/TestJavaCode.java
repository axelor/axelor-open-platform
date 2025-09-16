/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestJavaCode {

  private String render(JavaCode code) {
    JavaContext ctx = new JavaContext("com.example");
    JavaWriter headWriter = new JavaWriter(ctx, "");
    JavaWriter codeWriter = new JavaWriter(ctx, "  ");

    String codeText = codeWriter.emit(code).toString();
    String headText = headWriter.emit(ctx).toString();

    return headText + codeText;
  }

  @Test
  public void testString() {
    JavaCode code = new JavaCode("var name = {0:s}", "Some Name");
    String text = render(code);
    String expected = "" + "package com.example;\n" + "\n" + "var name = \"Some Name\"";
    assertEquals(expected, text);
  }

  @Test
  public void testType() {
    JavaCode code =
        new JavaCode(
            "{0:t}<String> names = new {1:t}<>()", "java.util.List", "java.util.ArrayList");

    String text = render(code);
    String expected =
        """
        package com.example;

        import java.util.ArrayList;
        import java.util.List;

        List<String> names = new ArrayList<>()""";

    assertEquals(expected, text);
  }

  @Test
  public void testMember() {
    JavaCode code = new JavaCode("final var value = {0:m};", "com.some.Example.NAME");
    String text = render(code);
    String expected =
        """
        package com.example;

        import com.some.Example;

        final var value = Example.NAME;\
        """;

    assertEquals(expected, text);
  }

  @Test
  public void testStatic() {
    JavaCode code = new JavaCode("{0:M}({1:s});", "System.out.println", "Hello!");
    String text = render(code);
    String expected =
        """
        package com.example;

        import static System.out.println;

        println("Hello!");\
        """;
    assertEquals(expected, text);
  }
}
