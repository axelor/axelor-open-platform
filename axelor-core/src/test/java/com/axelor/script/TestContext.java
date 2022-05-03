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
package com.axelor.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.inject.Beans;
import com.axelor.rpc.Context;
import com.axelor.rpc.ContextEntity;
import com.axelor.test.db.Contact;
import com.axelor.test.db.TypeCheck;
import com.axelor.test.db.repo.ContactRepository;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestContext extends ScriptTest {

  public static final String STATIC_FIELD = "A static value...";
  public static final String HELLO_MESSAGE = "Hello world...";

  private String message = HELLO_MESSAGE;

  public static String staticMethod() {
    return STATIC_FIELD;
  }

  public String hello() {
    return message;
  }

  public Contact contact() {
    return Beans.get(ContactRepository.class).all().fetchOne();
  }

  @Test
  public void testContext() throws Exception {

    final Context context = new Context(contextMap(), Contact.class);
    final Contact proxy = context.asType(Contact.class);
    final Contact managed = getEntityManager().find(Contact.class, proxy.getId());

    assertNotNull(proxy.getTitle());
    assertEquals("Mrs. John NAME", proxy.getFullName());

    proxy.setFirstName("Some");
    assertEquals("Mrs. Some NAME", proxy.getFullName());

    context.putAll(ImmutableMap.of("firstName", "Some1"));
    assertEquals("Mrs. Some1 NAME", proxy.getFullName());

    assertEquals("Mr. John Smith", managed.getFullName());

    assertNotNull(proxy.getAddresses());
    assertEquals(2, proxy.getAddresses().size());

    assertTrue(proxy.getAddresses().get(1) instanceof ContextEntity);
    assertTrue(proxy.getAddresses().get(1).isSelected());

    assertTrue(context.get("parentContext") instanceof Context);

    assertEquals(managed.getEmail(), proxy.getEmail());
    assertEquals(managed.getEmail(), context.get("email"));

    assertNotNull(proxy.getCircles());
    assertTrue(proxy.getCircles().size() > 0);
    assertFalse(proxy.getCircle(0) instanceof ContextEntity);

    assertTrue(proxy instanceof ContextEntity);
    assertNotNull(((ContextEntity) proxy).getContextEntity());
    assertNotNull(((ContextEntity) proxy).getContextMap());
  }

  @Test
  public void testBooleanAndIntegerFields() {
    Map<String, Object> data = new HashMap<>();
    Context context = new Context(data, TypeCheck.class);
    assertEquals(Boolean.FALSE, context.get("boolValue"));
    assertEquals(0, context.get("intValue"));
  }

  @Test
  public void testEL() {
    testConfig(new ELScriptHelper(new ScriptBindings(context())));
  }

  @Test
  public void testGroovy() {
    testConfig(new GroovyScriptHelper(new ScriptBindings(context())));
  }

  private void testConfig(ScriptHelper helper) {
    final Object hello = helper.eval("__config__.hello");
    final Object world = helper.eval("__config__.world");
    final Object result = helper.eval("__config__.hello.hello()");
    final Object contact = helper.eval("__config__.hello.contact()");

    assertNotNull(hello);
    assertNotNull(world);
    assertNotNull(result);
    assertNotNull(contact);

    assertTrue(hello instanceof TestContext);
    assertEquals(HELLO_MESSAGE, world);
    assertEquals(HELLO_MESSAGE, result);
    assertTrue(contact instanceof Contact);

    final Object some = helper.eval("__config__.some");
    final Object thing = helper.eval("__config__.thing");
    final Object flag = helper.eval("__config__.flag");
    final Object string = helper.eval("__config__.string");
    final Object number = helper.eval("__config__.number");

    assertNotNull(some);
    assertNotNull(thing);
    assertNotNull(flag);
    assertNotNull(string);
    assertNotNull(number);

    assertEquals(some, STATIC_FIELD);
    assertEquals(thing, STATIC_FIELD);
    assertEquals(flag, Boolean.TRUE);
    assertEquals(string, "some static text value");
    assertEquals(number, 100);
  }
}
