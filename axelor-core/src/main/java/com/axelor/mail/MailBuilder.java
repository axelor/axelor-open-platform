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

import static com.axelor.common.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.FileTypeMap;
import javax.activation.URLDataSource;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

/**
 * The {@link MailBuilder} defines fluent API to build {@link MimeMessage} and
 * if required can send the built message directly.
 *
 */
public final class MailBuilder {

	private Session session;

	private String subject;

	private String from = "";
	private String sender = "";

	private Set<String> toRecipients = new LinkedHashSet<>();
	private Set<String> ccRecipients = new LinkedHashSet<>();
	private Set<String> bccRecipients = new LinkedHashSet<>();
	private Set<String> replyRecipients = new LinkedHashSet<>();

	private final List<Content> contents = new ArrayList<>();

	private final Map<String, String> headers = new HashMap<>();

	private final FileTypeMap fileTypeMap = new MailFileTypeMap();

	private boolean hasText;
	private boolean hasHtml;
	private boolean hasInline;
	private boolean hasAttach;

	private class Content {
		String cid;
		String text;
		String name;
		String file;
		boolean inline;
		boolean html;

		public MimePart apply(MimePart message) throws MessagingException {
			if (text == null) return message;
			if (html) {
				message.setText(text, "UTF-8", "html");
			} else {
				message.setText(text);
			}
			return message;
		}
	}

	public MailBuilder(Session session) {
		this.session = session;
	}

	public MailBuilder subject(String subject) {
		this.subject = subject;
		return this;
	}

	private MailBuilder addAll(Collection<String> to, String... recipients) {
		Preconditions.checkNotNull(recipients, "recipients can't be null");
		for (String email : recipients) {
			Preconditions.checkNotNull(email, "email can't be null");
		}
		Collections.addAll(to, recipients);
		return this;
	}

	public MailBuilder to(String... recipients) {
		return addAll(toRecipients, recipients);
	}

	public MailBuilder cc(String... recipients) {
		Preconditions.checkNotNull(recipients, "recipients can't be null");
		return addAll(ccRecipients, recipients);
	}

	public MailBuilder bcc(String... recipients) {
		return addAll(bccRecipients, recipients);
	}

	public MailBuilder replyTo(String... recipients) {
		return addAll(replyRecipients, recipients);
	}

	public MailBuilder from(String from) {
		this.from = from;
		return this;
	}

	public MailBuilder sender(String sender) {
		this.sender = sender;
		return this;
	}

	public MailBuilder header(String name, String value) {
		Preconditions.checkNotNull(name, "header name can't be null");
		headers.put(name, value);
		return this;
	}

	public MailBuilder text(String text) {
		hasText = true;
		return text(text, false);
	}

	public MailBuilder html(String text) {
		hasHtml = true;
		return text(text, true);
	}

	private MailBuilder text(String text, boolean html) {
		Preconditions.checkNotNull(text, "text can't be null");
		Content content = new Content();
		content.text = text;
		content.html = html;
		contents.add(content);
		return this;
	}

	public MailBuilder attach(String name, String link) {
		return attach(name, link, null);
	}

	/**
	 * Attach a file referenced by the given link.
	 *
	 * <p>
	 * If you want to reference the attachment as inline image, provide content
	 * id wrapped by angle brackets and refer the image with content id without
	 * angle brackets.
	 * </p>
	 *
	 * For example:
	 *
	 * <pre>
	 * builder
	 * 	.html("<img src='cid:logo.png'>")
	 * 	.attach("logo.png", "/path/to/logo.png", "<logo.png>"
	 * 	.send();
	 * <pre>
	 *
	 * @param name
	 *            attachment file name
	 * @param link
	 *            attachment file link (url or file path)
	 * @param cid
	 *            content id
	 * @return this
	 */
	public MailBuilder attach(String name, String link, String cid) {
		Preconditions.checkNotNull(link, "link can't be null");
		Content content = new Content();
		content.name = name;
		content.file = link;
		content.cid = cid;
		contents.add(content);

		if (cid != null && cid.indexOf('<') == 0) {
			hasInline = true;
		} else {
			hasAttach = true;
		}

		return this;
	}

	/**
	 * Attach a file as inline content.
	 *
	 * @param name
	 *            attachment file name
	 * @param link
	 *            attachment file link (url or file path)
	 * @param inline
	 *            whether to inline this attachment
	 * @return this
	 */
	public MailBuilder inline(String name, String link) {
		Preconditions.checkNotNull(link, "link can't be null");
		Content content = new Content();
		content.name = name;
		content.file = link;
		content.cid = "<" + name + ">";
		content.inline = true;
		contents.add(content);
		hasInline = true;
		return this;
	}

	/**
	 * Build a new {@link MimeMessage} instance from the provided details.
	 *
	 * @return an instance of {@link MimeMessage}
	 * @throws MessagingException
	 * @throws IOException
	 */
	public MimeMessage build() throws MessagingException, IOException {
		return build(null);
	}

	/**
	 * Build a new {@link MimeMessage} instance from the provided details.
	 *
	 * @param messageId
	 *            custom "Message-ID" to use, null to use auto-generated
	 * @return an instance of {@link MimeMessage}
	 * @throws MessagingException
	 * @throws IOException
	 */
	public MimeMessage build(final String messageId) throws MessagingException, IOException {

		MimeMessage message = new MimeMessage(session) {
			@Override
			protected void updateMessageID() throws MessagingException {
				if (isBlank(messageId)) {
					super.updateMessageID();
				} else {
					this.setHeader("Message-ID", messageId);
				}
			}
		};

		message.setSubject(subject);
		message.setRecipients(RecipientType.TO, InternetAddress.parse(Joiner.on(",").join(toRecipients)));
		message.setRecipients(RecipientType.CC, InternetAddress.parse(Joiner.on(",").join(ccRecipients)));
		message.setRecipients(RecipientType.BCC, InternetAddress.parse(Joiner.on(",").join(bccRecipients)));

		message.setReplyTo(InternetAddress.parse(Joiner.on(",").join(replyRecipients)));

		if (!isBlank(from)) message.setFrom(new InternetAddress(from));
		if (!isBlank(sender)) message.setSender(new InternetAddress(sender));

		for (String name : headers.keySet()) {
			message.setHeader(name, headers.get(name));
		}

		// simple text or html email
		if (contents.size() == 1 && (hasText || hasHtml)) {
			contents.get(0).apply(message);
			return message;
		}

		final MimeMultipart rootContainer = new MimeMultipart();
		MimeMultipart inlineContainer = rootContainer;
		MimeMultipart bodyContainer = rootContainer;

		rootContainer.setSubType("mixed");

		// prepare multipart layout
		if (hasHtml && hasInline) {
			// if html & inline images are used, created related container
			inlineContainer = new MimeMultipart("related");
			bodyContainer = inlineContainer;
			addPart(rootContainer, inlineContainer, 0);

			// if text body is provided, create alternative container
			if (hasText) {
				bodyContainer = new MimeMultipart("alternative");
				addPart(inlineContainer, bodyContainer, 0);
			}
		} else if (hasHtml && hasText) {
			// create alternative container if html & text is provided and has attachments
			if (hasInline || hasAttach) {
				bodyContainer = new MimeMultipart("alternative");
				addPart(rootContainer, bodyContainer, 0);
			} else {
				// if no attachments, mark main container as alternative
				rootContainer.setSubType("alternative");
			}
		}

		final StringBuilder text = new StringBuilder();
		final StringBuilder html = new StringBuilder();

		for (Content content : contents) {
			if (content.text == null) {
				final MimeBodyPart part = new MimeBodyPart();
				try {
					final URL link = new URL(content.file);
					part.setDataHandler(new DataHandler(new URLDataSource(link)));
				} catch (MalformedURLException e) {
					// default implementation fails to detect mime type
					final FileDataSource fds = new FileDataSource(new File(content.file));
					fds.setFileTypeMap(fileTypeMap);
					part.setDataHandler(new DataHandler(fds));
				}
				part.setFileName(content.name);
				if (content.cid != null) {
					part.setContentID(content.cid);
				}
				if (content.inline) {
					part.setDisposition(Part.INLINE);
					inlineContainer.addBodyPart(part);
				} else {
					part.setDisposition(Part.ATTACHMENT);
					rootContainer.addBodyPart(part);
				}
			} else if (content.html) {
				html.append(content.text);
			} else {
				text.append(content.text);
			}
		}

		final MimeBodyPart htmlPart = new MimeBodyPart();
		final MimeBodyPart textPart = new MimeBodyPart();

		if (html.length() > 0) {
			htmlPart.setText(html.toString(), "UTF-8", "html");
			bodyContainer.addBodyPart(htmlPart, 0);
		}
		if (text.length() > 0) {
			textPart.setText(text.toString(), "UTF-8");
			bodyContainer.addBodyPart(textPart, 0);
		}

		message.setContent(rootContainer);

		return message;
	}

	private void addPart(MimeMultipart root, MimeMultipart content, int index) throws MessagingException {
		final BodyPart part = new MimeBodyPart();
		part.setContent(content);
		root.addBodyPart(part, index);
	}

	/**
	 * Send the message with given send date.
	 *
	 * @param date
	 *            send date, can be null
	 *
	 * @return sent {@link MimeMessage}
	 * @throws MessagingException
	 * @throws IOException
	 */
	public MimeMessage send(Date date) throws MessagingException, IOException {
		final MimeMessage message = build();
		try {
			message.setSentDate(date);
		} catch (Exception e) {
		}
		Transport.send(message);
		return message;
	}

	/**
	 * Send the message.
	 *
	 * @return sent {@link MimeMessage}
	 * @throws MessagingException
	 * @throws IOException
	 */
	public MimeMessage send() throws MessagingException, IOException {
		return send(new Date());
	}

}
