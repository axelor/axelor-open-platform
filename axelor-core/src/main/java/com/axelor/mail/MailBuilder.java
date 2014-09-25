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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.URLDataSource;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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

	private List<String> toRecipients = Lists.newArrayList();
	private List<String> ccRecipients = Lists.newArrayList();
	private List<String> bccRecipients = Lists.newArrayList();
	private List<String> replyRecipients = Lists.newArrayList();

	private List<Content> contents = Lists.newArrayList();
	
	private Map<String, String> headers = Maps.newHashMap();

	private boolean textOnly;

	private class Content {
		String text;
		String name;
		String file;
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

	public MailBuilder to(String... recipients) {
		Preconditions.checkNotNull(recipients, "recipients can't be null");
		this.toRecipients.addAll(Arrays.asList(recipients));
		return this;
	}

	public MailBuilder cc(String... recipients) {
		Preconditions.checkNotNull(recipients, "recipients can't be null");
		this.ccRecipients.addAll(Arrays.asList(recipients));
		return this;
	}

	public MailBuilder bcc(String... recipients) {
		Preconditions.checkNotNull(recipients, "recipients can't be null");
		this.bccRecipients.addAll(Arrays.asList(recipients));
		return this;
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
		return text(text, false);
	}

	public MailBuilder html(String text) {
		return text(text, true);
	}

	private MailBuilder text(String text, boolean html) {
		Preconditions.checkNotNull(text, "text can't be null");
		Content content = new Content();
		content.text = text;
		content.html = html;
		contents.add(content);
		textOnly = contents.size() == 1;
		return this;
	}

	public MailBuilder attach(String name, String link) {
		Preconditions.checkNotNull(link, "link can't be null");
		Content content = new Content();
		content.name = name;
		content.file = link;
		contents.add(content);
		textOnly = false;
		return this;
	}

	public MimeMessage build() throws MessagingException, IOException {

		MimeMessage message = new MimeMessage(session);

		message.setSubject(subject);
		message.setRecipients(RecipientType.TO, InternetAddress.parse(Joiner.on(",").join(toRecipients)));
		message.setRecipients(RecipientType.CC, InternetAddress.parse(Joiner.on(",").join(ccRecipients)));
		message.setRecipients(RecipientType.BCC, InternetAddress.parse(Joiner.on(",").join(bccRecipients)));

		message.setReplyTo(InternetAddress.parse(Joiner.on(",").join(replyRecipients)));
		
		if (!"".equals(from)) message.setFrom(new InternetAddress(from));
		if (!"".equals(sender)) message.setSender(new InternetAddress(sender));
		
		for (String name : headers.keySet()) {
			message.setHeader(name, headers.get(name));
		}

		if (textOnly) {
			contents.get(0).apply(message);
			return message;
		}

		Multipart mp = new MimeMultipart();
		for (Content content : contents) {
			MimeBodyPart part = new MimeBodyPart();
			if (content.text == null) {
				part.setFileName(content.name);
				try {
					URL link = new URL(content.file);
					part.setDataHandler(new DataHandler(new URLDataSource(link)));
				} catch (MalformedURLException e) {
					part.attachFile(content.file);
				}
			} else {
				content.apply(part);
			}
			mp.addBodyPart(part);
		}
		
		message.setContent(mp);

		return message;
	}

	public void send() throws MessagingException, IOException {
		Transport.send(build());
	}
}
