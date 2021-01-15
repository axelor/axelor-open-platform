/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
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
import org.junit.Assert;
import org.junit.Test;

public class TestJsonContext extends ScriptTest {

  private void testCustomFields(Context context, String json) throws Exception {
    final ScriptHelper engine = new GroovyScriptHelper(context);

    Assert.assertEquals(json, context.asType(Contact.class).getAttrs());
    Assert.assertTrue(engine.eval("$attrs") instanceof JsonContext);

    Assert.assertTrue(engine.eval("$attrs.guardian") instanceof Contact);
    Assert.assertTrue(engine.eval("$attrs.guardian.fullName") instanceof String);

    Assert.assertEquals("Some NAME", engine.eval("$attrs.name = 'Some NAME'"));
    Assert.assertEquals(context.get("attrs"), context.asType(Contact.class).getAttrs());
    Assert.assertNotEquals(json, context.asType(Contact.class).getAttrs());
    Assert.assertTrue(context.asType(Contact.class).getAttrs().contains("Some NAME"));

    Assert.assertFalse(context.asType(Contact.class).getAttrs().contains("date"));
    Assert.assertNotNull(engine.eval("$attrs.birthDate = __time__"));
    Assert.assertTrue(context.asType(Contact.class).getAttrs().contains("birthDate"));

    context.put("guardian", null);
    Assert.assertFalse(context.asType(Contact.class).getAttrs().contains("guardian"));

    context.put("guardian", all(Contact.class).fetchOne());
    Assert.assertTrue(context.asType(Contact.class).getAttrs().contains("guardian"));

    try {
      context.put("guardian", new Contact());
      Assert.fail();
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

    Assert.assertNotNull(hello.getAttrs());
    Assert.assertNotNull(world.getAttrs());
    Assert.assertEquals("Hello!!!", hello.getName());
    Assert.assertEquals("World!!!", world.getName());

    Assert.assertTrue(hello.getAttrs().contains("date"));
    Assert.assertTrue(hello.getAttrs().contains("world"));
    Assert.assertTrue(hello.getAttrs().contains("World!!!"));
    Assert.assertTrue(world.getAttrs().contains("1000.25"));

    final Context ctx = $json.create(hello);
    Assert.assertEquals("Hello!!!", ctx.get("name"));
    Assert.assertTrue(ctx.get("world") instanceof Map);

    final ScriptHelper sh = new NashornScriptHelper(ctx);
    final Object name = sh.eval("name");
    Assert.assertEquals("Hello!!!", name);
    Assert.assertNotNull(sh.eval("contact"));
    Assert.assertTrue(sh.eval("world") instanceof MetaJsonRecord);
    Assert.assertTrue(sh.eval("world.price") instanceof BigDecimal);
  }
}
