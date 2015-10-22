/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
package com.axelor.mail.db.repo;

import com.axelor.db.JpaRepository;
import com.axelor.mail.db.MailAddress;

public class MailAddressRepository extends JpaRepository<MailAddress> {

	public MailAddressRepository() {
		super(MailAddress.class);
	}

	public MailAddress findByEmail(String email) {
		return all().filter("self.address = :email")
				.bind("email", email)
				.cacheable()
				.fetchOne();
	}

	public MailAddress findOrCreate(String email) {
		return findOrCreate(email, email);
	}

	public MailAddress findOrCreate(String email, String displayName) {
		MailAddress address = findByEmail(email);
		if (address == null) {
			address = new MailAddress();
			address.setAddress(email);
			address.setPersonal(displayName);
			save(address);
		}
		return address;
	}
}
