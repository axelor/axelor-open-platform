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

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.axelor.common.ResourceUtils;

public class MailBuilderTest extends AbstractMailTest {

	private MailSender sender;

	@Override
	@Before
	public void startServer() {
		if (sender == null) {
			sender = new MailSender(SMTP_ACCOUNT);
		}
	}

	private MimeMessage sendAndRecieve(Message message) throws Exception {
		sender.send(message);
		server.waitForIncomingEmail(1);
		return server.getReceivedMessages()[0];
	}

	@Test
	public void testPlain() throws Exception {

		Message message = sender.compose()
				.to("me@localhost")
				.to("you@localhost", "else@localhost")
				.bcc("you@localhost")
				.subject("Hello...")
				.text("Hello!!!")
				.build();

		MimeMessage msg = sendAndRecieve(message);

		Assert.assertEquals(3, message.getRecipients(RecipientType.TO).length);
		Assert.assertEquals(1, message.getRecipients(RecipientType.BCC).length);

		Assert.assertTrue(msg.getContent() instanceof String);

		String content = (String) msg.getContent();

		Assert.assertEquals("Hello...", msg.getSubject());
		Assert.assertEquals("Hello!!!", content.trim());
	}

	@Test
	public void testMultipart() throws Exception {

		Message message = sender.compose()
				.to("me@localhost")
				.bcc("you@localhost")
				.subject("Hello...")
				.text("Hello!!!")
				.text("World!!!")
				.build();

		MimeMessage msg = sendAndRecieve(message);

		Assert.assertEquals("Hello...", msg.getSubject());
		Assert.assertTrue(msg.getContent() instanceof Multipart);

		Multipart content = (Multipart) msg.getContent();

		Assert.assertEquals(1, content.getCount());

		for (int i = 0; i < content.getCount(); i++) {
			BodyPart part = content.getBodyPart(i);
			Assert.assertTrue(part.getContent() instanceof String);
		}
	}

	@Test
	public void testAttachment() throws Exception {

		String file = ResourceUtils.getResource("log4j.properties").getFile();
		String url = "file://" + file;

		Message message = sender.compose()
				.to("me@localhost")
				.bcc("you@localhost")
				.subject("Hello...")
				.text("Hello!!!")
				.attach("log4j.properties", file)
				.attach("log4j.properties", url)
				.build();

		MimeMessage msg = sendAndRecieve(message);

		Assert.assertTrue(msg.getContent() instanceof Multipart);

		Multipart content = (Multipart) msg.getContent();

		Assert.assertEquals(3, content.getCount());

		for (int i = 1; i < content.getCount(); i++) {
			BodyPart part = content.getBodyPart(i);
			Assert.assertNotNull(part.getFileName());
			Assert.assertNotNull(part.getContent());
		}
	}
}
