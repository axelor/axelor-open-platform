/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.axelor.inject.Beans;
import com.axelor.rpc.Context;
import com.axelor.rpc.ContextEntity;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Invoice;
import com.axelor.test.db.Title;
import com.axelor.test.db.repo.ContactRepository;
import com.google.common.collect.ImmutableMap;

public class TestContext extends ScriptTest {

	public static final String STATIC_FIELD = "A static value...";
	public static final String HELLO_MESSAGE = "Hello world...";

	private String message = HELLO_MESSAGE;
	
	private Contact contact;
	private Title title;

	@Before
	public void init() {
		if (contact == null) {
			contact = all(Contact.class).filter("self.email = ?", "jsmith@gmail.com").fetchOne();
			title = all(Title.class).filter("self.code = ?", "mrs").fetchOne();
		}
	}
	
	public static String staticMethod() {
		return STATIC_FIELD;
	}

	public String hello() {
		return message;
	}

	public Contact contact() {
		ContactRepository repo = Beans.get(ContactRepository.class);
		return repo.all().fetchOne();
	}

	private Map<String, Object> contextMap() {

		final Map<String, Object> values = new HashMap<>();
		values.put("lastName", "NAME");
		values.put("id", contact.getId());
		
		final Map<String, Object> t = new HashMap<>();
		t.put("id", title.getId());
		values.put("title", t);

		final List<Map<String, Object>> addresses = new ArrayList<>();
		final Map<String, Object> a1 = new HashMap<>();
		a1.put("street", "My");
		a1.put("area", "Home");
		a1.put("city", "Paris");
		a1.put("zip", "1212");
		final Map<String, Object> a2 = new HashMap<>();
		a2.put("street", "My");
		a2.put("area", "Office");
		a2.put("city", "London");
		a2.put("zip", "1111");
		a2.put("selected", true);
		
		addresses.add(a1);
		addresses.add(a2);

		values.put("addresses", addresses);
		
		final Map<String, Object> parent = new HashMap<>();
		parent.put("_model", Invoice.class.getName());
		parent.put("date", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

		values.put("_parent", parent);

		return values;
	}
	
	@Test
	public void testContext() throws Exception {

		final Context ctx = new Context(contextMap(), Contact.class);
		final Contact cnt = ctx.asLazyType(Contact.class);

		Assert.assertNotNull(cnt.getTitle());
		Assert.assertEquals("Mrs. John NAME", cnt.getFullName());

		cnt.setFirstName("Some");
		Assert.assertEquals("Mrs. Some NAME", cnt.getFullName());

		ctx.putAll(ImmutableMap.of("firstName", "Some1"));
		Assert.assertEquals("Mrs. Some1 NAME", cnt.getFullName());

		Assert.assertEquals("Mr. John Smith", contact.getFullName());
		
		Assert.assertNotNull(cnt.getAddresses());
		Assert.assertEquals(2, cnt.getAddresses().size());
		
		Assert.assertTrue(cnt.getAddresses().get(1) instanceof ContextEntity);
		Assert.assertTrue(cnt.getAddresses().get(1).isSelected());

		Assert.assertTrue(ctx.get("parentContext") instanceof Context);
		
		Assert.assertEquals(contact.getEmail(), cnt.getEmail());
		Assert.assertEquals(contact.getEmail(), ctx.get("email"));

		Assert.assertNotNull(cnt.getCircles());
		Assert.assertTrue(cnt.getCircles().size() > 0);
		Assert.assertFalse(cnt.getCircle(0) instanceof ContextEntity);
		
		Assert.assertTrue(cnt instanceof ContextEntity);
		Assert.assertNotNull(((ContextEntity) cnt).getContextEntity());
	}

	@Test
	public void testConfigContext() {

		final ScriptBindings bindings = new ScriptBindings(this.context());
		final ScriptHelper helper = new GroovyScriptHelper(bindings);

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
