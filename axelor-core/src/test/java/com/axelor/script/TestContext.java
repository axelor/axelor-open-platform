/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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
import org.junit.Test;

import com.axelor.inject.Beans;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;

public class TestContext extends ScriptTest {

	public static final String STATIC_FIELD = "A static value...";

	private String message = "Hello world...";

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

	@Test
	public void testConfigContext() {

		ScriptBindings bindings = new ScriptBindings(this.context());
		ScriptHelper helper = new GroovyScriptHelper(bindings);

		Object hello = helper.eval("__config__.hello");
		Object world = helper.eval("__config__.world");
		Object result = helper.eval("__config__.hello.hello()");
		Object contact = helper.eval("__config__.hello.contact()");

		Assert.assertNotNull(hello);
		Assert.assertNotNull(world);
		Assert.assertNotNull(result);
		Assert.assertNotNull(contact);

		Assert.assertTrue(hello instanceof TestContext);
		Assert.assertEquals(message, world);
		Assert.assertEquals(message, result);
		Assert.assertTrue(contact instanceof Contact);

		Object some = helper.eval("__config__.some");
		Object thing = helper.eval("__config__.thing");
		Object flag = helper.eval("__config__.flag");
		Object string = helper.eval("__config__.string");
		Object number = helper.eval("__config__.number");

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
