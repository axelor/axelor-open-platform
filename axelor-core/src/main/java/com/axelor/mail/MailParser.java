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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimePartDataSource;
import javax.mail.internet.ParseException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * Parses a {@link MimeMessage} and stores the individual parts such as context
 * text, attachments etc.
 *
 */
public final class MailParser {

	private final MimeMessage message;

	private String text;

	private String html;

	private String summary;

	private boolean isMultiPart;

	private List<DataSource> attachments;

	/**
	 * Create a new {@link MailParser} for the given {@link MimeMessage}.
	 *
	 * @param message
	 *            the {@link MimeMessage} to parse
	 */
	public MailParser(MimeMessage message) {
		this.message = message;
		this.attachments = new ArrayList<>();
	}

	/**
	 * Parse the message.
	 *
	 * @return the {@link MailParser} instance itself
	 * @throws MessagingException
	 * @throws IOException
	 */
	public MailParser parse() throws MessagingException, IOException {
		this.parse(message);
		return this;
	}

	private List<InternetAddress> getRecipients(RecipientType type) throws MessagingException {
		final List<InternetAddress> result = new ArrayList<>();
		final InternetAddress[] all = (InternetAddress[]) this.message.getRecipients(type);
		if (all != null) {
			Collections.addAll(result, all);
		}
		return result;
	}

	/**
	 * Get list of "to" recipients of the message.
	 *
	 * @return list of {@link InternetAddress}
	 * @throws MessagingException
	 *             if unable to get addresses
	 */
	public List<InternetAddress> getTo() throws MessagingException {
		return getRecipients(RecipientType.TO);
	}

	/**
	 * Get list of "cc" recipients of the message.
	 *
	 * @return list of {@link InternetAddress}
	 * @throws MessagingException
	 */
	public List<InternetAddress> getCc() throws MessagingException {
		return getRecipients(RecipientType.CC);
	}

	/**
	 * Get list of "bcc" recipients of the message.
	 *
	 * @return list of {@link InternetAddress}
	 * @throws MessagingException
	 */
	public List<InternetAddress> getBcc() throws MessagingException {
		return getRecipients(RecipientType.BCC);
	}

	/**
	 * Get "from" address of the message.
	 *
	 * @return {@link InternetAddress} or null if unable to read "from" field
	 * @throws MessagingException
	 */
	public InternetAddress getFrom() throws MessagingException {
		final InternetAddress[] all = (InternetAddress[]) message.getFrom();
		if (all == null || all.length == 0) {
			return null;
		}
		return all[0];
	}

	/**
	 * Get "replyTo" address of the message.
	 *
	 * @return {@link InternetAddress} or null unable to read "replyTo" field.
	 * @throws MessagingException
	 */
	public InternetAddress getReplyTo() throws MessagingException {
		final InternetAddress[] all = (InternetAddress[]) message.getReplyTo();
		if (all == null || all.length == 0) {
			return null;
		}
		return all[0];
	}

	/**
	 * Get the message subject line.
	 *
	 * @return message subject line
	 * @throws MessagingException
	 */
	public String getSubject() throws MessagingException {
		return message.getSubject();
	}

	/**
	 * Get plain text content if available.
	 *
	 */
	public String getText() {
		if (text == null && html != null) {
			text = toPlainText(html);
		}
		return text;
	}

	/**
	 * Get the html content if available.
	 *
	 */
	public String getHtml() {
		return html;
	}

	/**
	 * Get the first line of the email as summary.
	 * @return
	 */
	public String getSummary() {
		if (summary == null && getText() != null) {
			final String text = getText();
			summary = text.substring(0, text.indexOf("\n"));
		}
		return summary;
	}

	public String getSafeHtml() {
		if (html == null) {
			return null;
		}
		return sanitize(html);
	}

	/**
	 * Whether the message is multipart message.
	 *
	 */
	public boolean isMultiPart() {
		return isMultiPart;
	}

	/**
	 * Whether the message has attachments.
	 *
	 */
	public boolean hasAttachments() {
		return !attachments.isEmpty();
	}

	/**
	 * Get attachments.
	 *
	 * @return list of {@link DataSource}
	 */
	public List<DataSource> getAttachments() {
		return attachments;
	}

	/**
	 * Get the first header value for the the header name.
	 *
	 * @param name
	 *            header name
	 * @return first header value if found else null
	 * @throws MessagingException
	 */
	public String getHeader(String name) throws MessagingException {
		final String[] all = message.getHeader(name);
		if (all == null || all.length == 0) {
			return null;
		}
		return all[0];
	}

	private boolean isMimeType(MimePart part, String type) throws MessagingException {
		try {
			final ContentType contentType = new ContentType(part.getDataHandler().getContentType());
			return contentType.match(type);
		} catch (ParseException e) {
			return part.getContentType().equalsIgnoreCase(type);
		}
	}

	private void parse(MimePart part) throws MessagingException, IOException {
		final String disposition = part.getDisposition();
		final boolean isAttachment = disposition != null && disposition.contains(Part.ATTACHMENT);

		if (text == null && isMimeType(part, "text/plain") && !isAttachment) {
			this.text = (String) part.getContent();
			return;
		}
		if (html == null && isMimeType(part, "text/html") && !isAttachment) {
			this.html = (String) part.getContent();
			return;
		}

		if (isMimeType(part, "multipart/*")) {
			this.isMultiPart = true;
			final Multipart parts = (Multipart) part.getContent();
			final int count = parts.getCount();
			for (int i = 0; i < count; i++) {
				parse((MimeBodyPart) parts.getBodyPart(i));
			}
		} else {
			final DataSource dataSource = new MimePartDataSource(part);
			attachments.add(dataSource);
		}
	}

	private String toPlainText(String html) {
		final Element doc = Jsoup.parse(html);
		final FormattingVisitor formatter = new FormattingVisitor();
		final NodeTraversor traversor = new NodeTraversor(formatter);
		traversor.traverse(doc);
		return formatter.toString();
	}

	private String sanitize(String html) {
		return Jsoup.clean(html, Whitelist.basicWithImages());
	}

	private static final class FormattingVisitor implements NodeVisitor {

		private final StringBuilder builder = new StringBuilder();

		private void newLine() {
			int n = builder.length();
			if (n < 2 || builder.charAt(n - 1) != '\n' || builder.charAt(n - 2) != '\n') {
				builder.append("\n");
			}
		}

		@Override
		public void head(Node node, int depth) {
			if (node instanceof TextNode) {
				builder.append(((TextNode) node).text());
				return;
			}
			final String name = node.nodeName();
			switch (name) {
			case "li":
				builder.append("\n * ");
				break;
			case "dt":
				builder.append("  ");
				break;
			case "p":
			case "div":
			case "blockquote":
			case "h1":
			case "h2":
			case "h3":
			case "h4":
			case "h5":
			case "tr":
				newLine();
				break;
			}
		}

		@Override
		public void tail(Node node, int depth) {
			final String name = node.nodeName();
			switch (name) {
			case "a":
				builder.append(String.format(" <%s>", node.attr("href")));
				break;
			case "p":
			case "div":
			case "blockquote":
			case "br":
			case "dd":
			case "dt":
			case "h1":
			case "h2":
			case "h3":
			case "h4":
			case "h5":
				newLine();
				break;
			}
		}

		@Override
		public String toString() {
			return builder.toString().trim();
		}
	}
}
