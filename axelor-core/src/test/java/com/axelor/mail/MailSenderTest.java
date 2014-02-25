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
