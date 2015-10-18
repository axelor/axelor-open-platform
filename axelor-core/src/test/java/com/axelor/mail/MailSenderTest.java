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
package com.axelor.mail;

import java.io.IOException;
import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Test;

import com.axelor.common.ClassUtils;

public class MailSenderTest extends AbstractMailTest {

	private static final String SMTP_HOST = "smtp.gmail.com";
	private static final String SMTP_PORT = "587";

	private static final String SMTP_USER = "my.name@gmail.com";
	private static final String SMTP_PASS = "secret";

	private static final String SEND_TO = "my.name@gmail.com";

	private static final String TEXT = "<strong>Hello world...</strong>"
			+ "<hr>"
			+ "<p>This is a testing email and not a <strong><span style='color: red;'>spam...</span></strong></p>"
			+ "<p>This is a link image...</p>"
			+ "<img src='http://www.axelor.com/wp-content/uploads/2014/08/Logo-site.png'>"
			+ "<p>This is logo1...</p>"
			+ "<img src='cid:logo1.png'></img>"  // show logo1.png as inline image
			+ "<br>"
			+ "<p>This is logo2...</p>"
			+ "<img src='cid:logo2.png'></img>"  // show logo2.png as inline image
			+ "<br>"
			+ "<p>This is logo3...</p>"
			+ "<img src='cid:logo3.png'></img>"  // try to show logo3.png as inline image (should not show)
			+ "<br>"
			+ "---"
			+ "<span style='color: blue;'><i>John Smith</i></span>";

	private void send(MailAccount account, Date sentOn) throws MessagingException, IOException {

		final MailSender sender = new MailSender(account);

		final String file = ClassUtils.getResource("com/axelor/mail/test-file.txt").getFile();
		final String image = ClassUtils.getResource("com/axelor/mail/test-image.png").getFile();

		sender.compose()
			.to(SEND_TO)
			.subject("Hello...")
			.html(TEXT)
			.attach("text.txt", file)
			.inline("logo1.png", image) // attach logo as inline image
			.attach("logo2.png", image, "<logo2.png>") // attach logo as attachment with content id
			.attach("logo3.png", image) // attach logo as attachment without content id
			.send(sentOn);
	}

	@Test
	public void testReal() throws Exception {
		if ("secret".equals(SMTP_PASS)) {
			return;
		}
		final MailAccount account = new SmtpAccount(SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SmtpAccount.CHANNEL_STARTTLS);
		send(account, null);
	}

	@Test
	public void testLocal() throws Exception {

		final Date sentOn = new LocalDateTime().withMillisOfSecond(0).minusDays(15).toDate();

		send(SMTP_ACCOUNT, sentOn);

		Assert.assertNotNull(server.getReceivedMessages());
		Assert.assertTrue(server.getReceivedMessages().length > 0);

		final MimeMessage m1 = server.getReceivedMessages()[0];

		Assert.assertNotNull(m1);
		Assert.assertEquals("Hello...", m1.getSubject());
		Assert.assertEquals(sentOn, m1.getSentDate());
		Assert.assertTrue(m1.getContent() instanceof MimeMultipart);

		final MimeMultipart parts = (MimeMultipart) m1.getContent();

		Assert.assertEquals(5, parts.getCount());

		// test body part
		final MimeBodyPart part1 = (MimeBodyPart) parts.getBodyPart(0);
		Assert.assertTrue(part1.getContentType().contains("text/html"));
		Assert.assertTrue(part1.getContent() instanceof String);
		Assert.assertTrue(TEXT.equals(part1.getContent()));

		// test first attachment
		final MimeBodyPart part2 = (MimeBodyPart) parts.getBodyPart(1);
		Assert.assertEquals("text.txt", part2.getFileName());
		Assert.assertEquals("Hello...", part2.getContent());

		// test inline attachment
		final MimeBodyPart part3 = (MimeBodyPart) parts.getBodyPart(2);
		Assert.assertEquals("logo1.png", part3.getFileName());
		Assert.assertTrue(part3.getContentType().contains("image/png"));
		Assert.assertTrue(part3.getDisposition().contains("inline"));

		// test image attachment with content-id
		final MimeBodyPart part4 = (MimeBodyPart) parts.getBodyPart(3);
		Assert.assertEquals("logo2.png", part4.getFileName());
		Assert.assertTrue(part4.getContentType().contains("image/png"));
		Assert.assertTrue(part4.getDisposition().contains("attachment"));

		// test image attachment without content-id
		final MimeBodyPart part5 = (MimeBodyPart) parts.getBodyPart(4);
		Assert.assertEquals("logo3.png", part5.getFileName());
		Assert.assertTrue(part5.getContentType().contains("image/png"));
		Assert.assertTrue(part5.getDisposition().contains("attachment"));
	}
}
