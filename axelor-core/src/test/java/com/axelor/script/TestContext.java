/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.axelor.inject.Beans;
import com.axelor.rpc.Context;
import com.axelor.rpc.ContextEntity;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import com.google.common.collect.ImmutableMap;

@FixMethodOrder(MethodSorters.JVM)
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

		Assert.assertNotNull(proxy.getTitle());
		Assert.assertEquals("Mrs. John NAME", proxy.getFullName());

		proxy.setFirstName("Some");
		Assert.assertEquals("Mrs. Some NAME", proxy.getFullName());

		context.putAll(ImmutableMap.of("firstName", "Some1"));
		Assert.assertEquals("Mrs. Some1 NAME", proxy.getFullName());

		Assert.assertEquals("Mr. John Smith", managed.getFullName());
		
		Assert.assertNotNull(proxy.getAddresses());
		Assert.assertEquals(2, proxy.getAddresses().size());
		
		Assert.assertTrue(proxy.getAddresses().get(1) instanceof ContextEntity);
		Assert.assertTrue(proxy.getAddresses().get(1).isSelected());

		Assert.assertTrue(context.get("parentContext") instanceof Context);
		
		Assert.assertEquals(managed.getEmail(), proxy.getEmail());
		Assert.assertEquals(managed.getEmail(), context.get("email"));

		Assert.assertNotNull(proxy.getCircles());
		Assert.assertTrue(proxy.getCircles().size() > 0);
		Assert.assertFalse(proxy.getCircle(0) instanceof ContextEntity);
		
		Assert.assertTrue(proxy instanceof ContextEntity);
		Assert.assertNotNull(((ContextEntity) proxy).getContextEntity());
		Assert.assertNotNull(((ContextEntity) proxy).getContextMap());
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

		Assert.assertNotNull(hello);
		Assert.assertNotNull(world);
		Assert.assertNotNull(result);
		Assert.assertNotNull(contact);

		Assert.assertTrue(hello instanceof TestContext);
		Assert.assertEquals(HELLO_MESSAGE, world);
		Assert.assertEquals(HELLO_MESSAGE, result);
		Assert.assertTrue(contact instanceof Contact);

		final Object some = helper.eval("__config__.some");
		final Object thing = helper.eval("__config__.thing");
		final Object flag = helper.eval("__config__.flag");
		final Object string = helper.eval("__config__.string");
		final Object number = helper.eval("__config__.number");

		Assert.assertNotNull(some);
		Assert.assertNotNull(thing);
		Assert.assertNotNull(flag);
		Assert.assertNotNull(string);
		Assert.assertNotNull(number);

		Assert.assertEquals(some, STATIC_FIELD);
		Assert.assertEquals(thing, STATIC_FIELD);
		Assert.assertEquals(flag, Boolean.TRUE);
		Assert.assertEquals(string, "some static text value");
		Assert.assertEquals(number, 100);
	}
}
