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
