/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2020 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.text;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.axelor.common.ResourceUtils;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.meta.db.repo.MetaJsonRecordRepository;
import com.axelor.rpc.Context;
import com.axelor.script.ScriptTest;
import com.axelor.test.db.Contact;
import com.google.inject.persist.Transactional;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class TemplateTest extends ScriptTest {

  private static final String GROOVY_TEMPLATE_SPECIAL =
      "<?mso-application progid=\"Word.Document\"?> \\@ ${firstName} \\\"${lastName}\\\" = \\* ${nested.message}";

  private static final String GROOVY_TEMPLATE_SIMPLE =
      "Hello: ${firstName} ${lastName} = ${nested.message}";

  private static final String STRING_TEMPLATE_SIMPLE =
      "Hello: <firstName> <lastName> = <nested.message>";

  private static final String STRING_TEMPLATE_COMPLEX =
      ""
          + "<html>\n"
          + "<style>\n"
          + "h1 { font-size: 14px; }\n"
          + "</style>\n"
          + "<body>\n"
          + "   <h1>Welcome!</h1>\n"
          + "   <ul>\n"
          + "       <li>Name: $x.fullName$</li>\n"
          + "       <li>Email: <a href='$x.email$'>$x.email$</a></li>\n"
          + "       <li>Type: $x.contactType$</li>\n"
          + "       <li>Status: $x.contactStatus$</li>\n"
          + "   </ul>\n"
          + "   <ul>\n"
          + "       <li>Nick: $x.nickName$</li>\n"
          + "       <li>Numerology: $x.numerology$</li>\n"
          + "       <li>Birthdate: $x.birthDate$</li>\n"
          + "       <li>Favorite Color: $x.favColor$</li>\n"
          + "       $if(x.guardian.id)$"
          + "       <li>Guardian: $x.guardian.fullName$</li>\n"
          + "       $endif$"
          + "   </ul>\n"
          + "</body>\n"
          + "</html>\n";

  private static final String STRING_TEMPLATE_JSON =
      ""
          + "<html>\n"
          + "<body>\n"
          + "   <h1 style=\"font-size: 14px;\">Hello!</h1>\n"
          + "   <ul>\n"
          + "       <li>Name: $x.name$</li>\n"
          + "       <li>Date: $x.date$</li>\n"
          + "       <li>Color: $x.color$</li>\n"
          + "       <li>Contact Name: $x.contact.fullName$</li>\n"
          + "       <li>Contact Type: $x.contact.contactType$</li>\n"
          + "   </ul>\n"
          + "   <ul>\n"
          + "       <li>World Name: $x.world.name$</li>\n"
          + "       <li>World Price: $x.world.price$</li>\n"
          + "   </ul>\n"
          + "</body>\n"
          + "</html>\n";

  private static final String OUTPUT_SIMPLE = "Hello: John Smith = Hello World!!!";

  private Map<String, Object> vars;

  @Override
  protected void prepareMoreData() {
    vars = new HashMap<>();
    vars.put("message", "Hello World!!!");

    vars.put("firstName", "John");
    vars.put("lastName", "Smith");

    vars.put("nested", new HashMap<>(vars));
  }

  @Test
  public void testGroovyInclude() throws Exception {

    InputStream stream = ResourceUtils.getResourceStream("com/axelor/text/include-test.tmpl");
    Reader reader = new InputStreamReader(stream);

    Templates templates = new GroovyTemplates();
    Template template = templates.from(reader);

    String output = template.make(vars).render();

    assertNotNull(output);
    assertTrue(output.indexOf("{{<") == -1);
    assertTrue(output.contains("This is nested 1"));
    assertTrue(output.contains("This is nested 2"));
  }

  @Test
  public void testGroovySpecial() {

    Templates templates = new GroovyTemplates();
    Template template = templates.fromText(GROOVY_TEMPLATE_SPECIAL);

    try {
      template.make(vars).render();
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testGroovyTemplate() {

    Templates templates = new GroovyTemplates();
    Template template = templates.fromText(GROOVY_TEMPLATE_SIMPLE);

    String text = template.make(vars).render();
    Assert.assertEquals(OUTPUT_SIMPLE, text);
  }

  @Test
  public void testStringTemplateSimple() {

    Templates templates = new StringTemplates();
    Template template = templates.fromText(STRING_TEMPLATE_SIMPLE);

    String text = template.make(vars).render();
    Assert.assertEquals(OUTPUT_SIMPLE, text);
  }

  @Test
  public void testStringTemplateComplex() {
    StringTemplates st = new StringTemplates('$', '$').withLocale(Locale.FRENCH);
    Template tmpl = st.fromText(STRING_TEMPLATE_COMPLEX);

    Context context = context();
    Map<String, Object> vars = new HashMap<>();

    vars.put("x", context);

    String output = tmpl.make(vars).render();

    Assert.assertTrue(output.contains("Type: Customer"));
    Assert.assertTrue(output.contains("Status: One"));
    Assert.assertTrue(output.contains("Nick: Some Name"));
    Assert.assertTrue(output.contains("Favorite Color: Red"));
    Assert.assertTrue(output.contains("Guardian:"));
  }

  @Test
  @Transactional
  public void testStringTemplateJson() {
    final StringTemplates st = new StringTemplates('$', '$').withLocale(Locale.FRENCH);
    final Template tmpl = st.fromText(STRING_TEMPLATE_JSON);

    final MetaJsonRecordRepository $json = Beans.get(MetaJsonRecordRepository.class);

    final Context helloCtx = $json.create("hello");
    helloCtx.put("name", "Hello!!!");
    helloCtx.put("date", LocalDateTime.now());
    helloCtx.put("color", "red");

    final Contact contact = all(Contact.class).fetchOne();

    contact.setContactType("customer");
    helloCtx.put("contact", contact);

    final Context worldCtx = $json.create("world");
    worldCtx.put("name", "World!!!");
    worldCtx.put("price", 1000.25);

    final MetaJsonRecord world = $json.save(worldCtx);

    helloCtx.put("world", world);

    final MetaJsonRecord hello = $json.save(helloCtx);
    final Context context = $json.create(hello);

    vars.put("x", context);

    String output = tmpl.make(vars).render();

    System.err.println("QQQ: " + output);

    Assert.assertTrue(output.contains("Name: Hello!!!"));
    Assert.assertTrue(output.contains("Contact Type: Customer"));
    Assert.assertTrue(output.contains("World Name: World!!!"));
    Assert.assertTrue(output.contains("World Price: 1000.25"));
  }
}
