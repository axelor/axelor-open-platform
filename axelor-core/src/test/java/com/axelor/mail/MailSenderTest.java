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

import java.io.IOException;
import java.util.Date;

import javax.mail.MessagingException;

import org.junit.Test;

import com.axelor.AbstractTest;
import com.axelor.common.ClassUtils;

public class MailSenderTest extends AbstractTest {

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
		final MailAccount account = new SmtpAccount(SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SmtpAccount.ENCRYPTION_TLS);
		send(account, null);
	}
}
