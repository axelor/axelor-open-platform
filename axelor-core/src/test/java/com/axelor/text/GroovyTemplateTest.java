/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
