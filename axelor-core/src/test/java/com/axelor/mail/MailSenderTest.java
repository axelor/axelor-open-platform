/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.Assert;
import org.junit.Test;

import com.axelor.common.ClassUtils;

public class MailSenderTest extends AbstractMailTest {

	private static final String SMTP_HOST = "smtp.gmail.com";
	private static final String SMTP_PORT = "587";

	private static final String SMTP_USER = "my.name@gmail.com";
	private static final String SMTP_PASS = "secret";

	private static final String SEND_TO = "my.name@gmail.com";

	private static final String HTML = ""
			+ "<strong>Hello world...</strong>"
			+ "<hr>"
			+ "<p>This is a testing email and not a <strong><span style='color: red;'>spam...</span></strong></p>"
			+ "<p>This is logo1...</p>"
			+ "<img src='cid:logo1.png'></img>"  // show logo1.png as inline image
			+ "<br>"
			+ "---"
			+ "<span style='color: blue;'><i>John Smith</i></span>";

	private static final String TEXT =""
			+ "Hello world...\n"
			+ "--------------\n\n"
			+ "This is a testing email and not a *spam...*\n\n"
			+ "---\n"
			+ "John Smith";

	private void send(MailAccount account, Date sentOn) throws MessagingException, IOException {

		final MailSender sender = new MailSender(account);

		final String file = ClassUtils.getResource("com/axelor/mail/test-file.txt").getFile();
		final String image = ClassUtils.getResource("com/axelor/mail/test-image.png").getFile();

		sender.compose()
			.to(SEND_TO)
			.subject("Hello...")
			.text(TEXT)
			.html(HTML)
			.attach("text.txt", file)
			.inline("logo1.png", image)
			.send(sentOn);
	}

	@Test
	public void testReal() throws Exception {
		if ("secret".equals(SMTP_PASS)) {
			return;
		}
		final MailAccount account = new SmtpAccount(SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, MailConstants.CHANNEL_STARTTLS);
		send(account, null);
	}

	@Test
	public void testLocal() throws Exception {

		final Date sentOn = Date.from(LocalDateTime.now().minusDays(15).toInstant(ZoneOffset.UTC));

		send(SMTP_ACCOUNT, sentOn);

		Assert.assertNotNull(server.getReceivedMessages());
		Assert.assertTrue(server.getReceivedMessages().length > 0);

		final MimeMessage m1 = server.getReceivedMessages()[0];

		Assert.assertNotNull(m1);
		Assert.assertEquals("Hello...", m1.getSubject());
		Assert.assertEquals(sentOn, m1.getSentDate());
		Assert.assertTrue(m1.getContent() instanceof MimeMultipart);

		final MimeMultipart parts = (MimeMultipart) m1.getContent();

		Assert.assertEquals(2, parts.getCount());

		// test multipart/related
		final MimeBodyPart part1 = (MimeBodyPart) parts.getBodyPart(0);
		Assert.assertTrue(part1.getContentType().contains("multipart/related"));
		Assert.assertTrue(part1.getContent() instanceof MimeMultipart);

		final MimeMultipart related = (MimeMultipart) part1.getContent();

		Assert.assertEquals(2, related.getCount());
		Assert.assertTrue(related.getBodyPart(0).getContent() instanceof MimeMultipart);

		final MimeMultipart alternative = (MimeMultipart) related.getBodyPart(0).getContent();
		Assert.assertEquals(2, related.getCount());

		final MimeBodyPart textPart = (MimeBodyPart) alternative.getBodyPart(0);
		final MimeBodyPart htmlPart = (MimeBodyPart) alternative.getBodyPart(1);

		Assert.assertTrue(textPart.getContent() instanceof String);
		Assert.assertTrue(htmlPart.getContent() instanceof String);

		final String text = (String) textPart.getContent();
		final String html = (String) htmlPart.getContent();

		Assert.assertEquals(TEXT.trim().replace("\n", "\r\n"), text);
		Assert.assertEquals(HTML.trim().replace("\n", "\r\n"), html);

		// test inline attachment
		final MimeBodyPart inline = (MimeBodyPart) related.getBodyPart(1);
		Assert.assertEquals("logo1.png", inline.getFileName());
		Assert.assertTrue(inline.getContentType().contains("image/png"));
		Assert.assertTrue(inline.getDisposition().contains("inline"));

		// test attachment
		final MimeBodyPart part2 = (MimeBodyPart) parts.getBodyPart(1);
		Assert.assertEquals("text.txt", part2.getFileName());
		Assert.assertEquals("Hello...", part2.getContent());
	}
}
