/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
      """
      <html>
      <style>
      h1 { font-size: 14px; }
      </style>
      <body>
         <h1>Welcome!</h1>
         <ul>
             <li>Name: $x.fullName$</li>
             <li>Email: <a href='$x.email$'>$x.email$</a></li>
             <li>Type: $x.contactType$</li>
             <li>Status: $x.contactStatus$</li>
         </ul>
         <ul>
             <li>Nick: $x.nickName$</li>
             <li>Numerology: $x.numerology$</li>
             <li>Birthdate: $x.birthDate$</li>
             <li>Favorite Color: $x.favColor$</li>
      $if(x.guardian.id)$\
             <li>Guardian: $x.guardian.fullName$</li>
      $endif$\
         </ul>
      </body>
      </html>
      """;

  private static final String OUTPUT_COMPLEX =
      """
      <html>
      <style>
      h1 { font-size: 14px; }
      </style>
      <body>
         <h1>Welcome!</h1>
         <ul>
             <li>Name: Mrs. John NAME</li>
             <li>Email: <a href='jsmith@gmail.com'>jsmith@gmail.com</a></li>
             <li>Type: Customer</li>
             <li>Status: One</li>
         </ul>
         <ul>
             <li>Nick: Some Name</li>
             <li>Numerology: 2</li>
             <li>Birthdate: 2020-05-22</li>
             <li>Favorite Color: Red</li>
             <li>Guardian: Mr. Mark Ram</li>
         </ul>
      </body>
      </html>
      """;

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
      """
      <html>
      <body>
         <h1 style="font-size: 14px;">Hello!</h1>
         <ul>
             <li>Name: $x.name$</li>
             <li>Date: $x.date$</li>
             <li>Color: $x.color$</li>
             <li>Contact Name: $x.contact.fullName$</li>
             <li>Contact Type: $x.contact.contactType$</li>
         </ul>
         <ul>
             <li>World Name: $x.world.name$</li>
             <li>World Price: $x.world.price$</li>
         </ul>
      </body>
      </html>
      """;

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
