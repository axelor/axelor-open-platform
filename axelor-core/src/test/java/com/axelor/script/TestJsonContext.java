/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.meta.db.repo.MetaJsonRecordRepository;
import com.axelor.rpc.Context;
import com.axelor.rpc.JsonContext;
import com.axelor.test.db.Contact;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.Test;

public class TestJsonContext extends ScriptTest {

  private void testCustomFields(Context context, String json) throws Exception {
    final ScriptHelper engine = new GroovyScriptHelper(context);

    assertEquals(json, context.asType(Contact.class).getAttrs());
    assertTrue(engine.eval("$attrs") instanceof JsonContext);

    assertTrue(engine.eval("$attrs.guardian") instanceof Contact);
    assertTrue(engine.eval("$attrs.guardian.fullName") instanceof String);

    assertEquals("Some NAME", engine.eval("$attrs.name = 'Some NAME'"));
    assertEquals(context.get("attrs"), context.asType(Contact.class).getAttrs());
    assertNotEquals(json, context.asType(Contact.class).getAttrs());
    assertTrue(context.asType(Contact.class).getAttrs().contains("Some NAME"));

    assertFalse(context.asType(Contact.class).getAttrs().contains("date"));
    assertNotNull(engine.eval("$attrs.birthDate = __time__"));
    assertTrue(context.asType(Contact.class).getAttrs().contains("birthDate"));

    context.put("guardian", null);
    assertFalse(context.asType(Contact.class).getAttrs().contains("guardian"));

    context.put("guardian", all(Contact.class).fetchOne());
    assertTrue(context.asType(Contact.class).getAttrs().contains("guardian"));

    try {
      context.put("guardian", new Contact());
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  public void testCustomFieldsUnmanaged() throws Exception {
    final Map<String, Object> values = new HashMap<>();
    final String json = getCustomerAttrsJson();
    values.put("attrs", json);
    testCustomFields(new Context(values, Contact.class), json);
  }

  @Test
  @Transactional
  public void testCustomFieldsManaged() throws Exception {
    final EntityManager em = getEntityManager();
    final Contact contact = new Contact("Test", "NAME");
    final String json = getCustomerAttrsJson();

    contact.setAttrs(json);

    em.persist(contact);
    em.flush();

    testCustomFields(new Context(contact.getId(), Contact.class), json);
  }

  @Test
  @Transactional
  public void testCustomModels() throws Exception {
    final MetaJsonRecordRepository $json = Beans.get(MetaJsonRecordRepository.class);

    final Context helloCtx = $json.create("hello");
    helloCtx.put("name", "Hello!!!");
    helloCtx.put("date", LocalDateTime.now());
    helloCtx.put("color", "red");

    final Contact contact = all(Contact.class).fetchOne();
    helloCtx.put("contact", contact);

    final Context worldCtx = $json.create("world");
    worldCtx.put("name", "World!!!");
    worldCtx.put("price", 1000.25);

    final MetaJsonRecord world = $json.save(worldCtx);

    helloCtx.put("world", world);

    final MetaJsonRecord hello = $json.save(helloCtx);

    assertNotNull(hello.getAttrs());
    assertNotNull(world.getAttrs());
    assertEquals("Hello!!!", hello.getName());
    assertEquals("World!!!", world.getName());

    assertTrue(hello.getAttrs().contains("date"));
    assertTrue(hello.getAttrs().contains("world"));
    assertTrue(hello.getAttrs().contains("World!!!"));
    assertTrue(world.getAttrs().contains("1000.25"));

    final Context ctx = $json.create(hello);
    assertEquals("Hello!!!", ctx.get("name"));
    assertTrue(ctx.get("world") instanceof Map);

    final ScriptHelper sh = new JavaScriptScriptHelper(ctx);
    final Object name = sh.eval("name");
    assertEquals("Hello!!!", name);
    assertNotNull(sh.eval("contact"));
    assertTrue(sh.eval("world") instanceof MetaJsonRecord);
    assertTrue(sh.eval("world.price") instanceof BigDecimal);
  }
}
