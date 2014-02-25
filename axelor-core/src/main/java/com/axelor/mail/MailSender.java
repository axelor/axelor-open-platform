/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.mail;

import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

/**
 * The {@link MailSender} provides features to send mails.
 * 
 */
public final class MailSender {

	private Session session;

	/**
	 * Create a new {@link MailSender} with the given account.
	 * 
	 * @param account
	 *            the account to use
	 */
	public MailSender(MailAccount account) {
		this.session = account.getSession();
	}

	/**
	 * Compose a new mail message.
	 * 
	 * @return a {@link MailBuilder} instance.
	 */
	public MailBuilder compose() {
		return new MailBuilder(session);
	}

	/**
	 * Send a mail message.
	 * 
	 * @param message
	 *            the message to sent
	 * @throws MessagingException
	 *             if the message could not be sent
	 * @see Transport#send(Message)
	 */
	public void send(Message message) throws MessagingException {
		Transport.send(message);
	}

	/**
	 * Send a simple text message to the given recipients.<br>
	 * <br>
	 * 
	 * For better options, use {@link #compose()} method to compose a message
	 * using {@link MailBuilder} which can be retrieved or sent directly with
	 * {@link MailBuilder#build()} or {@link MailBuilder#send()} methods.
	 * 
	 * @param subject
	 *            the message subject
	 * @param text
	 *            the message text
	 * @param recipients
	 *            list of recipients
	 * @throws MessagingException
	 *             if the message could not be sent
	 */
	public void send(String subject, String text, String... recipients) throws MessagingException {
		try {
			compose().subject(subject).to(recipients).text(text).send();
		} catch (IOException e) {
		}
	}
}
