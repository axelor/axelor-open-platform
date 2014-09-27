/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
package com.axelor.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestInflector {
	
	private Inflector inflector = Inflector.getInstance();

	//TODO: more tests

	@Test
	public void testSingular() {
		assertEquals("child", inflector.singularize("children"));
		assertEquals("post", inflector.singularize("posts"));
	}

	@Test
	public void testPlural() {
		assertEquals("children", inflector.pluralize("child"));
		assertEquals("posts", inflector.pluralize("post"));
	}

	@Test
	public void testOthers() {
		
		assertEquals("address_book", inflector.underscore("AddressBook"));
		assertEquals("address_book", inflector.underscore("Address    Book"));
		assertEquals("address_book", inflector.underscore("address-book"));
		
		assertEquals("AddressBook", inflector.camelize("address_book", false));
		assertEquals("addressBook", inflector.camelize("address-book", true));
		assertEquals("AddressBook", inflector.camelize("address book", false));

		assertEquals("Contact", inflector.humanize("contact_id"));
		
		assertEquals("1st", inflector.ordinalize(1));
		assertEquals("2nd", inflector.ordinalize(2));
		assertEquals("3rd", inflector.ordinalize(3));
		assertEquals("100th", inflector.ordinalize(100));
		assertEquals("103rd", inflector.ordinalize(103));
	}

	@Test
	public void testSimplify() {
		String source = "àèé schön";
		String target = "aee schon";
		assertEquals(target, inflector.simplify(source));
	}
}
