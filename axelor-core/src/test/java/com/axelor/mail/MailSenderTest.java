/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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

		MailAccount account = new SmtpAccount(SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SmtpAccount.ENCRYPTION_SSL);
		MailSender sender = new MailSender(account);

		String file = ClassUtils.getResource("log4j.properties").getFile();
		String url = "http://axelor.com/sites/default/files/logo.png";

		sender.compose()
			.to(SEND_TO)
			.subject("Hello...")
			.text("Hello!!!")
			.attach("log4j.properties", file)
			.attach("logo.png", url)
			.send();
	}
}
