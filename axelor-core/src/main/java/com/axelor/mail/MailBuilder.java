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
		Preconditions.checkNotNull(text, "text can't be null");
		Content content = new Content();
		content.text = text;
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
			String text = (String) contents.get(0).text;
			message.setText(text);
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
				part.setText(content.text);
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
