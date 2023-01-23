/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.common.ResourceUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.junit.jupiter.api.Test;

public class GroovyTemplateTest extends TemplateScriptTest {

  private static final String TEMPLATE_SIMPLE =
      "Hello: ${firstName} ${lastName} = ${nested.message}";
  private static final String OUTPUT_SIMPLE = "Hello: John Smith = Hello World!!!";

  @Test
  public void testGroovyTemplate() {

    Templates templates = new GroovyTemplates();
    Template template = templates.fromText(TEMPLATE_SIMPLE);

    String output = template.make(vars).render();
    assertEquals(OUTPUT_SIMPLE, output);
  }

  private static final String SPECIAL_TEMPLATE_SIMPLE =
      "<?mso-application progid=\"Word.Document\"?> \\@ ${firstName} \\\"${lastName}\\\" = \\* ${nested.message}";
  private static final String SPECIAL_OUTPUT_SIMPLE =
      "<?mso-application progid=\"Word.Document\"?> \\@ John \\\"Smith\\\" = \\* Hello World!!!";

  @Test
  public void testGroovySpecial() {

    Templates templates = new GroovyTemplates();
    Template template = templates.fromText(SPECIAL_TEMPLATE_SIMPLE);

    String output = template.make(vars).render();
    assertEquals(SPECIAL_OUTPUT_SIMPLE, output);
  }

  @Test
  public void testGroovyInclude() throws Exception {

    InputStream stream = ResourceUtils.getResourceStream("com/axelor/text/include-test.tmpl");
    Reader reader = new InputStreamReader(stream);

    Templates templates = new GroovyTemplates();
    Template template = templates.from(reader);

    String output = template.make(vars).render();

    assertNotNull(output);
    assertFalse(output.contains("{{<"));
    assertTrue(output.contains("This is nested 1"));
    assertTrue(output.contains("This is nested 2"));
  }
}
