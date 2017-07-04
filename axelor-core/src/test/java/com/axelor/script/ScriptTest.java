/*
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

import javax.inject.Inject;

import org.junit.Before;

import com.axelor.JpaTest;
import com.axelor.rpc.Context;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Title;
import com.axelor.test.db.repo.ContactRepository;
import com.axelor.test.db.repo.TitleRepository;
import com.google.inject.persist.Transactional;

public abstract class ScriptTest extends JpaTest {

	@Inject
	private ContactRepository contacts;
	
	@Inject
	private TitleRepository titles;
	
	private Contact contact;
	private Title title;

	@Before
	@Transactional
	public void prepare() {
		if (titles.all().count() == 0) {
			Title t = new Title();
			t.setCode("mr");
			t.setName("Mr.");
			titles.save(t);
		}
		if (contacts.all().count() == 0) {
			Contact c = new Contact();
			c.setFirstName("John");
			c.setLastName("Smith");
			c.setEmail("jsmith@gmail.com");
			c.setTitle(titles.findByCode("mr"));
			contacts.save(c);
		}
		
		if (contact == null) {
			contact = contacts.findByEmail("jsmith@gmail.com");
		}
		if (title == null) {
			title = titles.findByCode("mrs");
		}
	}

	protected Context context() {
		return new Context(contextMap(), Contact.class);
	}

	protected Map<String, Object> contextMap() {

		final Map<String, Object> values = new HashMap<>();
		values.put("lastName", "NAME");
		values.put("id", contact.getId());
		values.put("_model", Contact.class.getName());

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
		parent.put("_model", Contact.class.getName());
		parent.put("id", contact.getId());
		parent.put("date", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

		values.put("_parent", parent);
		values.put("_ref", parent);

		return values;
	}
}
