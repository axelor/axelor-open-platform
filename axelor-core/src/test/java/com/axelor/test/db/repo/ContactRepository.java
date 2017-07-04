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
package com.axelor.test.db.repo;

import java.util.Map;

import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.test.db.Contact;

public class ContactRepository extends JpaRepository<Contact> {

	public ContactRepository() {
		super(Contact.class);
	}

	public Contact findByEmail(String email) {
		return all().filter("self.email = ?", email).fetchOne();
	}

	public Contact edit(Map<String, Object> values) {
		return JPA.edit(Contact.class, values);
	}

	public Contact manage(Contact contact) {
		return JPA.manage(contact);
	}
}
