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
package com.axelor.script;

import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;

import com.axelor.JpaTest;
import com.axelor.rpc.Context;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

public abstract class ScriptTest extends JpaTest {

	@Inject
	private ContactRepository contacts;

	@Before
	@Transactional
	public void prepare() {
		if (contacts.all().count() > 0)
			return;
		Contact c = new Contact();
		c.setFirstName("John");
		c.setLastName("Smith");
		contacts.save(c);
	}

    protected Context context() {
        final Map<String, Object> data = Maps.newHashMap();
        data.put("_model", Contact.class.getName());
        data.put("firstName", "My");
        data.put("lastName", "Name");
        data.put("fullName", "Mr. My Name");
        data.put("title", ImmutableMap.of("name", "Mr.", "code", "mr"));

        Map<String, Object> ref = Maps.newHashMap();
        ref.put("_model", Contact.class.getName());
        ref.put("id", 1);

        data.put("_ref", ref);
        data.put("_parent", ref);

        return Context.create(data, Contact.class);
    }
}
