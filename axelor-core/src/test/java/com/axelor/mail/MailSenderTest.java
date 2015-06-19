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
package com.axelor.mail;

import org.joda.time.LocalDateTime;
import org.junit.Test;

import com.axelor.AbstractTest;
import com.axelor.common.ClassUtils;

public class MailSenderTest extends AbstractTest {

	private static final String SMTP_HOST = "smtp.gmail.com";
	private static final String SMTP_PORT = "587";

	private static final String SMTP_USER = "my.name@gmail.com";
	private static final String SMTP_PASS = "secret";

	private static final String SEND_TO = "my.name@gmail.com";

	@Test
	public void testReal() throws Exception {

		if ("secret".equals(SMTP_PASS)) {
			return;
		}

		MailAccount account = new SmtpAccount(SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SmtpAccount.ENCRYPTION_TLS);
		MailSender sender = new MailSender(account);

		String file = ClassUtils.getResource("com/axelor/mail/test-file.txt").getFile();
		String image = ClassUtils.getResource("com/axelor/mail/test-image.png").getFile();

		String text = "<strong>Hello world...</strong>"
				+ "<hr>"
				+ "<p>This is a testing email and not a <strong><span style='color: red;'>spam...</span></strong></p>"
				+ "<p>This is a link image...</p>"
				+ "<img src='http://www.axelor.com/wp-content/uploads/2014/08/Logo-site.png'>"
				+ "<p>This is an inline image...</p>"
				+ "<img src='cid:logo.png'></img>"  // refer <logo.png> (don't include angle brackets)
				+ "<br>"
				+ "---"
				+ "<span style='color: blue;'><i>John Smith</i></span>";

		sender.compose()
			.to(SEND_TO)
			.subject("Hello...")
			.html(text)
			.attach("text.txt", file)
			.attach("logo.png", image, "<logo.png>") // as hidden content to refer as "cid:data", note the angle brackets
			.send();

		sender.compose()
		.to(SEND_TO)
		.subject("Hello again...")
		.html(text)
		.attach("text.txt", file)
		.attach("logo.png", image)  // cid is not set, can't be referenced as inline image
		.send((new LocalDateTime().minusDays(15)).toDate());
	}
}
