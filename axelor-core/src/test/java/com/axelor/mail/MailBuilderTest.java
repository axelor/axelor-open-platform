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

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import com.axelor.AbstractTest;
import com.axelor.common.ClassUtils;

public class MailBuilderTest extends AbstractTest {

	private Wiser wiser;
	
	private static MailSender sender;
	
	@BeforeClass
	public static void createSender() {
		sender = new MailSender(new SmtpAccount("localhost", "8587"));
	}
	
	@Before
	public void startServer() {
		if (wiser == null) {
			wiser = new Wiser();
			wiser.setPort(8587);
		}
		wiser.start();
	}

	@After
	public void stopServer() {
		wiser.stop();
	}

	private MimeMessage sendAndRecieve(Message message) throws Exception {
		sender.send(message);
		WiserMessage wm = wiser.getMessages().get(0);
		return wm.getMimeMessage();
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
		
		Assert.assertEquals(2, content.getCount());
		
		for (int i = 0; i < content.getCount(); i++) {
			BodyPart part = content.getBodyPart(i);
			Assert.assertTrue(part.getContent() instanceof String);
		}
	}
	
	@Test
	public void testAttachment() throws Exception {

		String file = ClassUtils.getResource("log4j.properties").getFile();
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
