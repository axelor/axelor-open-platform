/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.common.ResourceUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  private static final String SPECIAL_PREFIX = "<?mso-application progid=\"Word.Document\"?> \\@ ";
  private static final String SPECIAL_TEMPLATE_SIMPLE =
      SPECIAL_PREFIX + "${firstName} \\\"${lastName}\\\" = \\* ${nested.message}";
  private static final String SPECIAL_OUTPUT_SIMPLE =
      SPECIAL_PREFIX + "John \\\"Smith\\\" = \\* Hello World!!!";

  @Test
  public void testGroovySpecial() {

    Templates templates = new GroovyTemplates();
    Template template = templates.fromText(SPECIAL_TEMPLATE_SIMPLE);

    String output = template.make(vars).render();
    assertEquals(SPECIAL_OUTPUT_SIMPLE, output);
  }

  // A template is compiled once and its compiled form shared by all its renderings: each one uses
  // its own values, and the variables a rendering assigns do not reach the next ones.
  @Test
  public void testCompiledTemplatesAreShared() throws Exception {
    Templates templates = new GroovyTemplates();

    for (String prefix : List.of("", SPECIAL_PREFIX)) {
      Template template =
          templates.fromText(prefix + "<% if (assign) { secret = name } %>${secret}");

      String first = template.make(new HashMap<>(Map.of("assign", true, "name", "first"))).render();
      assertTrue(first.endsWith("first"), () -> "Unexpected output: " + first);

      // the variable assigned by the first rendering is not seen by the second one
      String second =
          template
              .make(new HashMap<>(Map.of("assign", false, "name", "second", "secret", "none")))
              .render();
      assertTrue(second.endsWith("none"), () -> "Variable shared between renderings: " + second);
    }

    var engine = com.axelor.script.GroovyScriptSupport.createStringTemplateEngine();
    assertTrue(
        GroovyTemplates.compile(engine, "${a}") == GroovyTemplates.compile(engine, "${a}"),
        "Expected the template to be compiled once");
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

  @Test
  void testSecurityAppSettings() {
    var templates = new GroovyTemplates();

    for (var prefix : List.of("", SPECIAL_PREFIX)) {
      assertEquals(
          prefix + "dev",
          templates
              .fromText(
                  prefix
                      + "${__bean__(com.axelor.script.policy.ScriptAppSettings).getApplicationMode()}")
              .make(Map.of())
              .render(),
          "Should allow custom helper");

      var appSettingsRenderer =
          templates
              .fromText(prefix + "${com.axelor.app.AppSettings.get().get('db.test.url')}")
              .make(Map.of());
      assertThrows(
          IllegalArgumentException.class,
          appSettingsRenderer::render,
          "Should not allow unrestricted AppSettings");
    }
  }

  @Test
  void testSecurityBeanPolicy() {
    var templates = new GroovyTemplates();

    for (var prefix : List.of("", SPECIAL_PREFIX)) {
      assertEquals(
          prefix + "AllowedByConfiguration",
          templates
              .fromText(
                  prefix + "${com.axelor.script.policy.AllowedByConfiguration.myStaticMethod()}")
              .make(Map.of())
              .render(),
          "Static method should be allowed by configuration");

      assertEquals(
          prefix + "AllowedByAnnotation",
          templates
              .fromText(
                  prefix + "${__bean__(com.axelor.script.policy.AllowedByAnnotation).myMethod()}")
              .make(Map.of())
              .render(),
          "Instance method should be allowed by annotation");

      var beanRenderer =
          templates
              .fromText(
                  prefix
                      + "${com.axelor.inject.Beans.get(com.axelor.script.policy.AllowedByAnnotation).myMethod()}")
              .make(Map.of());
      assertThrows(
          IllegalArgumentException.class,
          beanRenderer::render,
          "Should not allow unrestricted Beans.get");
    }
  }

  @Test
  void testClosuresInTemplates() {
    var templates = new GroovyTemplates();
    var orderedValues = new java.util.LinkedHashMap<String, Object>();
    orderedValues.put("a", 1);
    orderedValues.put("b", 2);

    for (var prefix : List.of("", SPECIAL_PREFIX)) {
      assertEquals(
          prefix + "2,4,6",
          templates
              .fromText(prefix + "<% out << items.collect { it * 2 }.join(',') %>")
              .make(Map.of("items", List.of(1, 2, 3)))
              .render());
      assertEquals(
          prefix + "a=1;b=2;",
          templates
              .fromText(prefix + "<% values.each { k, v -> %>${k}=${v};<% } %>")
              .make(Map.of("values", orderedValues))
              .render());
      assertEquals(
          prefix + "X",
          templates
              .fromText(prefix + "<% out << [name: 'x'].with { name.toUpperCase() } %>")
              .make(Map.of())
              .render());
    }
  }

  @Test
  void testAnnotationsAreRejected() {
    var templates = new GroovyTemplates();

    for (var prefix : List.of("", SPECIAL_PREFIX)) {
      var text =
          prefix
              + "<% @groovy.transform.ASTTest(value = { throw new RuntimeException("
              + "'COMPILE-TIME') }) def x = 1 %>";
      // the template is compiled when it is made
      Throwable ex =
          assertThrows(
              RuntimeException.class, () -> templates.fromText(text).make(Map.of()).render());
      // The template engines report compilation errors as a message (including the template
      // source), so check the message rather than the exception types.
      String messages = "";
      for (Throwable c = ex; c != null; c = c.getCause()) {
        assertFalse(
            "COMPILE-TIME".equals(c.getMessage()), "The @ASTTest closure must not run: " + ex);
        messages += c.getMessage();
      }
      assertTrue(
          messages.contains("Annotation '@groovy.transform.ASTTest' not allowed"),
          () -> "Expected the annotation to be rejected, got: " + ex);
    }
  }

  @Test
  void testSandboxEscapesAreDenied() {
    var templates = new GroovyTemplates();
    var missingCmd = "axelor-nonexistent-binary-xyz";

    for (var prefix : List.of("", SPECIAL_PREFIX)) {
      for (var code :
          List.of(
              "'" + missingCmd + "'.with { execute() }",
              "'" + missingCmd + "'.invokeMethod('execute', null)",
              "use(System) { 'PATH'.getenv() }",
              "evaluate('1 + 1')",
              "[('__$$policy'): [check: { o -> o }]].with { Runtime.getRuntime() }",
              "def c = { -> getenv('PATH') }; c.delegate = System; c()")) {
        var renderer = templates.fromText(prefix + "<% " + code + " %>").make(Map.of());
        assertThrows(IllegalArgumentException.class, renderer::render, "Should not allow: " + code);
      }
    }
  }
}
