/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
        ""
            + "package com.example;\n"
            + "\n"
            + "import java.util.ArrayList;\n"
            + "import java.util.List;\n"
            + "\n"
            + "List<String> names = new ArrayList<>()";

    assertEquals(expected, text);
  }

  @Test
  public void testMember() {
    JavaCode code = new JavaCode("final var value = {0:m};", "com.some.Example.NAME");
    String text = render(code);
    String expected =
        ""
            + "package com.example;\n"
            + "\n"
            + "import com.some.Example;\n"
            + "\n"
            + "final var value = Example.NAME;";

    assertEquals(expected, text);
  }

  @Test
  public void testStatic() {
    JavaCode code = new JavaCode("{0:M}({1:s});", "System.out.println", "Hello!");
    String text = render(code);
    String expected =
        ""
            + "package com.example;\n"
            + "\n"
            + "import static System.out.println;\n"
            + "\n"
            + "println(\"Hello!\");";
    assertEquals(expected, text);
  }
}
