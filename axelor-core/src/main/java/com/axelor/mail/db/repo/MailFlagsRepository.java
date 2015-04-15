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
package com.axelor.mail.db.repo;

import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.axelor.mail.db.MailFlags;
import com.axelor.mail.db.MailMessage;

public class MailFlagsRepository extends JpaRepository<MailFlags> {

	public MailFlagsRepository() {
		super(MailFlags.class);
	}

	public MailFlags findBy(MailMessage message, User user) {
		return all().filter("self.message = :message AND self.user = :user")
				.bind("message", message)
				.bind("user", user).fetchOne();
	}
}
