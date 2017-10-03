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
package com.axelor.mail;

import javax.mail.Folder;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Test;

public class MailReaderTest extends AbstractMailTest {

	final MailSender sender = new MailSender(SMTP_ACCOUNT);
	final MailReader imapReader = new MailReader(IMAP_ACCOUNT);
	final MailReader pop3Reader = new MailReader(POP3_ACCOUNT);

	private void test(MailSender sender, MailReader reader) throws Exception {

		final MimeMessage msg = sender.compose()
				.from("me@localhost")
				.to("you@localhost")
				.subject("Hello...")
				.text("Hello!!!")
				.build();

		user.deliver(msg);
		server.waitForIncomingEmail(1);

		Store store = reader.getStore();
		Assert.assertNotNull(store);

		Folder folder =	store.getFolder("INBOX");
		Assert.assertNotNull(folder);

		folder.open(Folder.READ_ONLY);

		Assert.assertEquals(1, folder.getMessageCount());

		MimeMessage incoming = (MimeMessage) folder.getMessage(1);
		Assert.assertNotNull(incoming);
		Assert.assertEquals("Hello...", incoming.getSubject());

		MimeMessage reply = (MimeMessage) incoming.reply(false);
		reply.setText("This is a reply...");

		user.deliver(reply);
		server.waitForIncomingEmail(1);

		folder = store.getFolder("INBOX");
		folder.open(Folder.READ_ONLY);

		Assert.assertEquals(2, folder.getMessageCount());

		incoming = (MimeMessage) folder.getMessage(2);

		Assert.assertEquals("Re: Hello...", incoming.getSubject());
		Assert.assertEquals(msg.getMessageID(), incoming.getHeader("In-Reply-To", ""));
	}

	@Test
	public void testIMAP() throws Exception {
		test(sender, imapReader);
	}

	@Test
	public void testPOP3() throws Exception {
		test(sender, pop3Reader);
	}
}
