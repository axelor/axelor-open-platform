/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.meta.db.repo.MetaJsonRecordRepository;
import com.axelor.rpc.Context;
import com.axelor.test.db.Contact;
import com.google.inject.persist.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class STTemplateTest extends TemplateScriptTest {

  private static final String TEMPLATE_SIMPLE = "Hello: <firstName> <lastName> = <nested.message>";
  private static final String OUTPUT_SIMPLE = "Hello: John Smith = Hello World!!!";

  @Test
  public void testStringTemplateSimple() {

    Templates templates = new StringTemplates();
    Template template = templates.fromText(TEMPLATE_SIMPLE);

    String text = template.make(vars).render();
    assertEquals(OUTPUT_SIMPLE, text);
  }

  private static final String TEMPLATE_COMPLEX =
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
          + "$if(x.guardian.id)$"
          + "       <li>Guardian: $x.guardian.fullName$</li>\n"
          + "$endif$"
          + "   </ul>\n"
          + "</body>\n"
          + "</html>\n";

  private static final String OUTPUT_COMPLEX =
      ""
          + "<html>\n"
          + "<style>\n"
          + "h1 { font-size: 14px; }\n"
          + "</style>\n"
          + "<body>\n"
          + "   <h1>Welcome!</h1>\n"
          + "   <ul>\n"
          + "       <li>Name: Mrs. John NAME</li>\n"
          + "       <li>Email: <a href='jsmith@gmail.com'>jsmith@gmail.com</a></li>\n"
          + "       <li>Type: Customer</li>\n"
          + "       <li>Status: One</li>\n"
          + "   </ul>\n"
          + "   <ul>\n"
          + "       <li>Nick: Some Name</li>\n"
          + "       <li>Numerology: 2</li>\n"
          + "       <li>Birthdate: 2020-05-22</li>\n"
          + "       <li>Favorite Color: Red</li>\n"
          + "       <li>Guardian: Mr. Mark Ram</li>\n"
          + "   </ul>\n"
          + "</body>\n"
          + "</html>\n";

  @Test
  public void testStringTemplateComplex() {
    StringTemplates st = new StringTemplates('$', '$').withLocale(Locale.FRENCH);
    Template tmpl = st.fromText(TEMPLATE_COMPLEX);

    Context context = context();
    Map<String, Object> vars = new HashMap<>();
    vars.put("x", context);

    String output = tmpl.make(vars).render();
    assertEquals(OUTPUT_COMPLEX, output);
  }

  @Test
  public void testStringTemplateSimpleJsonFromContext() {
    String output =
        new StringTemplates()
            .fromText(
                "< attrs.favColor >, < favColor >, < favColor; format=\"selection:colors\" >, < anotherAttrs.favColor >")
            .make(context())
            .render();

    // handling `< favColor >` will not be formatted. Should use `attrs` prefix
    // else can use `selection` formatter
    assertEquals("Red, red, Red, Black", output);
  }

  @Test
  public void testStringTemplateSimpleJsonFromModelVar() {
    contact.setAttrs(getCustomerAttrsJson());
    contact.setAnotherAttrs(getCustomerAnotherAttrsJson());

    Map<String, Object> entity = new HashMap<>();
    entity.put(Contact.class.getSimpleName(), contact);

    String output =
        new StringTemplates()
            .fromText(
                "< Contact.attrs.favColor >, < Contact.favColor >, < Contact.anotherAttrs.favColor >")
            .make(entity)
            .render();

    assertEquals("Red, Red, Black", output);
  }

  @Test
  public void testStringTemplateSimpleJsonFromModel() {
    contact.setAttrs(getCustomerAttrsJson());
    contact.setAnotherAttrs(getCustomerAnotherAttrsJson());

    String output =
        new StringTemplates()
            .fromText(
                "< attrs.favColor >, < favColor >, < favColor; format=\"selection:colors\" >, < anotherAttrs.favColor >")
            .make(contact)
            .render();

    // handling `< favColor >` will not be formatted. Should use `attrs` prefix
    // else can use `selection` formatter
    assertEquals("Red, red, Red, Black", output);
  }

  @Test
  public void testStringTemplateSimpleJsonFromContextVar() {
    // Pass context in variable
    Map<String, Object> values = new HashMap<>();
    values.put("ctx", context());
    String output =
        new StringTemplates()
            .fromText("< ctx.attrs.favColor >, < ctx.favColor >, < ctx.anotherAttrs.favColor >")
            .make(values)
            .render();

    assertEquals("Red, Red, Black", output);
  }

  @Test
  public void testStringTemplateSimpleJsonFormat() {
    // Pass context in variable
    Map<String, Object> values = new HashMap<>();
    values.put("ctx", context());
    String output =
        new StringTemplates()
            .withLocale(Locale.FRANCE)
            .fromText(
                "< ctx.attrs.birthDate >, < ctx.attrs.guardian.id >, < ctx.attrs.guardian.fullName >, < ctx.attrs.isCustomer >, < ctx.attrs.orderAmount >, < ctx.attrs.lastActivity; format=\"dd/MM/yyyy HH:mm:ss\">")
            .make(values)
            .render();

    assertEquals("2020-05-22, 1, Mr. Mark Ram, true, 1000.20, 29/04/2021 07:57:00", output);
  }

  private static final String TEMPLATE_JSON =
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

  @Test
  @Transactional
  public void testStringTemplateJson() {
    final StringTemplates st = new StringTemplates('$', '$').withLocale(Locale.FRENCH);
    final Template tmpl = st.fromText(TEMPLATE_JSON);

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

    assertTrue(output.contains("Name: Hello!!!"));
    assertTrue(output.contains("Contact Type: Customer"));
    assertTrue(output.contains("World Name: World!!!"));
    assertTrue(output.contains("World Price: 1000.25"));
  }

  @Test
  void testStringTemplateSelection() {
    final StringTemplates st = new StringTemplates('$', '$');
    final Map<String, Object> vars = new HashMap<>();
    vars.put("x", context());

    assertEquals("Customer", st.fromText("$x.contactType$").make(vars).render());
    assertEquals("customer", st.fromText("$x.contactType.value$").make(vars).render());
    assertEquals("Customer", st.fromText("$x.contactType.title$").make(vars).render());
  }
}
