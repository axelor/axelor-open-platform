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
package com.axelor.mail.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.mail.internet.InternetAddress;

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
	 * <p>
	 * It sends email asynchronously using {@link ExecutorService} to prevent
	 * process blocking. However, clients can wait for action to complete by
	 * calling {@link Future#get()}.
	 * </p>
	 *
	 * @param message
	 *            the message to send
	 * @return a {@link Future} object
	 * @throws MailException
	 */
	Future<Boolean> send(MailMessage message) throws MailException;

	/**
	 * Fetch mails from remote mail server.
	 *
	 * <p>
	 * This method is meant to be called by a background job runner, running
	 * under database transaction with super user. The implementation should
	 * process all incoming email messages and may save them as
	 * {@link MailMessage}.
	 * </p>
	 *
	 * @return a {@link Future} object
	 * @throws MailException
	 */
	Future<Boolean> fetch() throws MailException;

	/**
	 * Search for email addresses matching the given text.
	 *
	 * @param match
	 *            the match text
	 * @param selected
	 *            already selected email addresses
	 * @param maxResults
	 *            maximum number of items to return
	 * @return list of {@link InternetAddress}
	 */
	List<InternetAddress> findEmails(String matching, List<String> selected, int maxResults);
}
