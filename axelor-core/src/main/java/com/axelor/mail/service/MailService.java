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
package com.axelor.mail.service;

import java.util.List;

import javax.mail.internet.InternetAddress;

import com.axelor.auth.db.User;
import com.axelor.db.Model;
import com.axelor.mail.MailException;
import com.axelor.mail.db.MailMessage;
import com.google.inject.ImplementedBy;

/**
 * The mail service defines interface for sending/reading mails.
 *
 */
@ImplementedBy(MailServiceImpl.class)
public interface MailService {

	/**
	 * Send a mail for the given {@link MailMessage}.
	 *
	 * @param message
	 *            the message to send
	 * @throws MailException
	 *             on failure
	 */
	void send(MailMessage message) throws MailException;

	/**
	 * Fetch mails from remote mail server.
	 *
	 * @throws MailException
	 *             on failure
	 */
	void fetch() throws MailException;

	/**
	 * Resolve the given email address to an associated entity.
	 *
	 * <p>
	 * Generally, it should resolve to the {@link User}, <code>Contact</code>,
	 * <code>Partner</code> or <code>Customer</code> that represents a contact.
	 * </p>
	 *
	 * <p>
	 * The default implementation resolves to the {@link User} records.
	 * </p>
	 *
	 * @param email
	 *            the email address to resolve
	 * @return associated entity or null if can't be resolved
	 */
	Model resolve(String email);

	/**
	 * Search for email addresses matching the given text.
	 *
	 * @param matching
	 *            the match text
	 * @param selected
	 *            already selected email addresses
	 * @param maxResults
	 *            maximum number of items to return
	 * @return list of {@link InternetAddress}
	 */
	List<InternetAddress> findEmails(String matching, List<String> selected, int maxResults);
}
