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
package com.axelor.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestInflector {
	
	private Inflector inflector = Inflector.getInstance();

	@Test
	public void testSingular() {
		assertEquals("child", inflector.singularize("children"));
		assertEquals("post", inflector.singularize("posts"));
		
		assertEquals("prognosis", inflector.singularize("prognoses"));
		assertEquals("Analysis", inflector.singularize("Analyses"));
		assertEquals("book", inflector.singularize("books"));
		assertEquals("person", inflector.singularize("people"));
		assertEquals("money", inflector.singularize("money"));
		
		assertEquals("octopus", inflector.singularize("octopi"));
		assertEquals("vertex", inflector.singularize("vertices"));
		assertEquals("ox", inflector.singularize("oxen"));
		assertEquals("Address", inflector.singularize("Addresses"));
		assertEquals("library", inflector.singularize("libraries"));
		
		assertEquals("user", inflector.singularize("users"));
		assertEquals("group", inflector.singularize("groups"));
		assertEquals("entity", inflector.singularize("entities"));
		assertEquals("message", inflector.singularize("messages"));
		assertEquals("activity", inflector.singularize("activities"));
		assertEquals("binary", inflector.singularize("binaries"));
		assertEquals("data", inflector.singularize("data"));
		
		assertEquals("user", inflector.singularize("user"));
		assertEquals("group", inflector.singularize("group"));
		assertEquals("entity", inflector.singularize("entity"));
		assertEquals("message", inflector.singularize("message"));
		assertEquals("activity", inflector.singularize("activity"));
		assertEquals("binary", inflector.singularize("binary"));
		assertEquals("stadium", inflector.singularize("stadiums"));
	}

	@Test
	public void testPlural() {
		assertEquals("children", inflector.pluralize("child"));
		assertEquals("posts", inflector.pluralize("post"));
		
		assertEquals("prognoses", inflector.pluralize("prognosis"));
		assertEquals("Analyses", inflector.pluralize("Analysis"));
		assertEquals("books", inflector.pluralize("book"));
		assertEquals("people", inflector.pluralize("person"));
		assertEquals("money", inflector.pluralize("money"));
		
		assertEquals("octopi", inflector.pluralize("octopus"));
		assertEquals("vertices", inflector.pluralize("vertex"));
		assertEquals("oxen", inflector.pluralize("ox"));
		assertEquals("Addresses", inflector.pluralize("Address"));
		assertEquals("libraries", inflector.pluralize("library"));
		
		assertEquals("libraries", inflector.pluralize("library"));
		assertEquals("libraries", inflector.pluralize("library"));
		assertEquals("libraries", inflector.pluralize("library"));
		assertEquals("libraries", inflector.pluralize("library"));
		assertEquals("libraries", inflector.pluralize("library"));
		assertEquals("libraries", inflector.pluralize("library"));
		assertEquals("libraries", inflector.pluralize("library"));
		
		assertEquals("users", inflector.pluralize("user") );
		assertEquals("groups", inflector.pluralize("group") );
		assertEquals("entities", inflector.pluralize("entity") );
		assertEquals("messages", inflector.pluralize("message") );
		assertEquals("activities", inflector.pluralize("activity") );
		assertEquals("binaries", inflector.pluralize("binary") );
		assertEquals("data", inflector.pluralize("data") );
		
		assertEquals("users", inflector.pluralize("users") );
		assertEquals("groups", inflector.pluralize("groups") );
		assertEquals("entities", inflector.pluralize("entities") );
		assertEquals("messages", inflector.pluralize("messages") );
		assertEquals("activities", inflector.pluralize("activities") );
		assertEquals("binaries", inflector.pluralize("binaries") );
		assertEquals("stadiums", inflector.pluralize("stadium"));
	}

	@Test
	public void testUnderscore() {
		assertEquals("address_book", inflector.underscore("AddressBook"));
		assertEquals("address_book_test", inflector.underscore("AddressBookTest"));
		assertEquals("address_book", inflector.underscore("Address    Book"));
		assertEquals("address_book", inflector.underscore("address-book"));
		assertEquals("address_book", inflector.underscore("addressBook"));
		assertEquals("address_book", inflector.underscore("address book"));
		assertEquals("product", inflector.underscore("Product"));
		assertEquals("area51_controller", inflector.underscore("Area51Controller"));
	}
	
	@Test
	public void testCamelize() {
		assertEquals("AddressBook", inflector.camelize("address_book", false));
		assertEquals("addressBook", inflector.camelize("address-book", true));
		assertEquals("AddressBook", inflector.camelize("address book", false));
		assertEquals("AddressBook", inflector.camelize("address book", false));
		assertEquals("AddressBook", inflector.camelize("addressBook", false));
		assertEquals("AddressBookTest", inflector.camelize("address_book_test", false));
		assertEquals("Product", inflector.camelize("product", false));
		assertEquals("Area51Controller", inflector.camelize("area51_controller", false));
	}
	
	@Test
	public void testHumanize() {
		assertEquals("Contact", inflector.humanize("contact_id"));
		assertEquals("Employee", inflector.humanize("employee_id"));
		assertEquals("Underground", inflector.humanize("underground"));
	}
	
	@Test
	public void testOrdinarize() {
		assertEquals("1st", inflector.ordinalize(1));
		assertEquals("2nd", inflector.ordinalize(2));
		assertEquals("3rd", inflector.ordinalize(3));
		assertEquals("100th", inflector.ordinalize(100));
		assertEquals("103rd", inflector.ordinalize(103));
		assertEquals("1000th", inflector.ordinalize(1000));
		assertEquals("1001st", inflector.ordinalize(1001));
		assertEquals("10013th", inflector.ordinalize(10013));
	}

	@Test
	public void testSimplify() {
		String source = "àèé schön";
		String target = "aee schon";
		assertEquals(target, inflector.simplify(source));
	}
	
	@Test
	public void testCapitalize() {
		assertEquals("Bar", inflector.capitalize("bar"));
		assertEquals("Foo bar baz", inflector.capitalize("foo bar baz"));
	}
	
	@Test
	public void testEllipsize() {
		assertEquals("...", inflector.ellipsize("foo", 0));
		assertEquals("...", inflector.ellipsize("foo", 2));
		assertEquals("bare", inflector.ellipsize("bare", 4));
		assertEquals("bare", inflector.ellipsize("bare", 5));
		assertEquals("foo...", inflector.ellipsize("foo bar baz", 6));
		assertEquals("foo ...", inflector.ellipsize("foo bar baz", 7));
		assertEquals("foo b...", inflector.ellipsize("foo bar baz", 8));
	}
}
