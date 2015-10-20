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
package com.axelor.mail.service;

import static com.axelor.common.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.activation.DataSource;
import javax.inject.Singleton;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuditableRunner;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import com.axelor.mail.ImapAccount;
import com.axelor.mail.MailBuilder;
import com.axelor.mail.MailConstants;
import com.axelor.mail.MailException;
import com.axelor.mail.MailParser;
import com.axelor.mail.MailReader;
import com.axelor.mail.MailSender;
import com.axelor.mail.SmtpAccount;
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.MailGroup;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailFollowerRepository;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaAttachmentRepository;
import com.axelor.text.GroovyTemplates;
import com.axelor.text.Template;
import com.axelor.text.Templates;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.inject.persist.Transactional;

/**
 * Default {@link MailService} implementation.
 *
 */
@Singleton
public class MailServiceImpl implements MailService, MailConstants {

	/**
	 * The default mail sender is configured from application configuration settings.
	 */
	private MailSender sender;

	/**
	 * The default mail reader is configured from application configuration settings.
	 */
	private MailReader reader;

	private boolean senderConfigured;
	private boolean readerConfigured;

	private ExecutorService executor = Executors.newCachedThreadPool();

	private Logger log = LoggerFactory.getLogger(MailService.class);

	private static final String NOTIFICATION = "notification";

	static final class FalseFuture implements Future<Boolean> {

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public Boolean get() throws InterruptedException, ExecutionException {
			return false;
		}

		@Override
		public Boolean get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			return false;
		}
	}

	public MailServiceImpl() {
	}

	/**
	 * Get {@link MailSender} to use sending the given message.
	 * <p>
	 * Can be overridden to provide different {@link MailSender} for different
	 * messages and object depending on the business requirements.
	 * </p>
	 *
	 * @param message
	 *            the message to send with the sender
	 * @param entity
	 *            the related entity, can be null if there is no related record
	 * @return a {@link MailSender}, null if not configured
	 */
	protected MailSender getMailSender(final MailMessage message, Model entity) {
		if (senderConfigured) {
			return sender;
		}
		try {
			return initSender();
		} finally {
			senderConfigured = true;
		}
	}

	private synchronized MailSender initSender() {

		final AppSettings settings = AppSettings.get();

		final String host = settings.get(CONFIG_SMTP_HOST);
		final String port = settings.get(CONFIG_SMTP_PORT);
		final String user = settings.get(CONFIG_SMTP_USER);
		final String pass = settings.get(CONFIG_SMTP_PASS);
		final String channel = settings.get(CONFIG_SMTP_CHANNEL);

		final int timeout = settings.getInt(CONFIG_SMTP_TIMEOUT, DEFAULT_TIMEOUT);
		final int connectionTimeout = settings.getInt(CONFIG_SMTP_CONNECTION_TIMEOUT, DEFAULT_TIMEOUT);

		if (isBlank(host)) {
			return null;
		}

		final SmtpAccount smtpAccount = new SmtpAccount(host, port, user, pass, channel);
		smtpAccount.setTimeout(timeout);
		smtpAccount.setConnectionTimeout(connectionTimeout);
		sender = new MailSender(smtpAccount);

		return sender;
	}

	/**
	 * Get {@link MailReader} to use sending the given message.
	 * <p>
	 * Can be overridden to provide {@link MailReader} configured differently
	 * (e.g. from database config).
	 * </p>
	 *
	 * @param message
	 *            the message to send with the sender
	 * @param entity
	 *            the related entity, can be null if there is no related record
	 * @return a {@link MailSender}, null if not configured
	 */
	protected MailReader getMailReader() {
		if (readerConfigured) {
			return reader;
		}
		try {
			return initReader();
		} finally {
			readerConfigured = true;
		}
	}

	private synchronized MailReader initReader() {

		final AppSettings settings = AppSettings.get();

		final String host = settings.get(CONFIG_IMAP_HOST);
		final String port = settings.get(CONFIG_IMAP_PORT);
		final String user = settings.get(CONFIG_IMAP_USER);
		final String pass = settings.get(CONFIG_IMAP_PASS);
		final String channel = settings.get(CONFIG_IMAP_CHANNEL);

		final int timeout = settings.getInt(CONFIG_IMAP_TIMEOUT, DEFAULT_TIMEOUT);
		final int connectionTimeout = settings.getInt(CONFIG_IMAP_CONNECTION_TIMEOUT, DEFAULT_TIMEOUT);

		if (isBlank(host)) {
			return null;
		}

		final ImapAccount account = new ImapAccount(host, port, user, pass, channel);
		account.setTimeout(timeout);
		account.setConnectionTimeout(connectionTimeout);
		reader = new MailReader(account);

		return reader;
	}

	/**
	 * Get the subject line for the message.
	 *
	 * <p>
	 * Custom implementation can overrider this method to prepare good subject
	 * line.
	 * </p>
	 *
	 * @param message
	 *            the message for which subject line is required
	 * @param entity
	 *            the related entity, can be null if there is no related record
	 * @return subject line
	 */
	protected String getSubject(final MailMessage message, Model entity) {
		if (message == null) {
			return null;
		}
		String subject = message.getSubject();
		if (subject != null && entity != null) {
			try {
				subject = Mapper.of(entity.getClass()).getNameField().get(entity).toString() + " - " + subject;
			} catch (Exception e) {
			}
		}
		if (subject == null) {
			subject = getSubject(message.getParent() == null ? message.getRoot() : message.getParent(), entity);
		}
		// in case of message groups
		if (subject == null && entity instanceof MailGroup) {
			subject = ((MailGroup) entity).getName();
		}
		if (message.getParent() != null && subject != null) {
			subject = "Re: " + subject;
		}
		return subject;
	}

	/**
	 * Get the list of recipient email addresses.
	 *
	 * <p>
	 * The default implementation returns email addresses of all followers.
	 * Custom implementation may include more per business requirement. For
	 * example, including customer email on sale order is confirmed.
	 * </p>
	 *
	 * @param message
	 *            the message to send
	 * @param entity
	 *            the related entity, can be null if there is no related record
	 * @return set of email addresses
	 */
	protected Set<String> recipients(final MailMessage message, Model entity) {
		final Set<String> recipients = new LinkedHashSet<>();
		final MailFollowerRepository followers = Beans.get(MailFollowerRepository.class);

		if (message.getRecipients() != null) {
			for (User user : message.getRecipients()) {
				recipients.add(user.getEmail());
			}
		}
		for (MailFollower follower : followers.findAll(message)) {
			if (follower.getUser() != null) {
				recipients.add(follower.getUser().getEmail());
			}
		}

		return Sets.filter(recipients, Predicates.notNull());
	}

	/**
	 * Apply a template to prepare message content.
	 *
	 * <p>
	 * The default implementation uses very basic template. Custom
	 * implementations can apply different templates depending on message type
	 * and related entity or even current customer.
	 * </p>
	 *
	 * @param message
	 *            the message to send
	 * @param entity
	 *            the related entity, can be null if there is no related record
	 * @return final message body text
	 * @throws IOException
	 *             if there is any error applying template
	 */
	protected String template(final MailMessage message, Model entity) throws IOException {

		final String text = message.getBody().trim();
		if (text == null || !NOTIFICATION.equals(message.getType()) || !(text.startsWith("{") || text.startsWith("}"))) {
			return text;
		}

		// audit tracking notification is stored as json data
		final ObjectMapper mapper = Beans.get(ObjectMapper.class);
		final Map<String, Object> map = mapper.readValue(text, new TypeReference<Map<String, Object>>() {});

		final Map<String, Object> data = new HashMap<>();

		data.put("audit", map);
		data.put("entity", entity);

		Templates templates = Beans.get(GroovyTemplates.class);
		Template tmpl = templates.fromText(""
				+ "<ul>"
				+ "<% for (def item : audit.tracks) { %>"
				+ "<li><strong>${item.title}</strong>: <span>${item.value}</span></li>"
				+ "<% } %>"
				+ "</ul>");

		return tmpl.make(data).render();
	}

	/**
	 * Find related entity.
	 *
	 * @param message
	 *            the message
	 * @return related entity or null if there is no related record
	 */
	protected final Model findEntity(final MailMessage message) {
		try {
			return (Model) JPA.em().find(Class.forName(message.getRelatedModel()), message.getRelatedId());
		} catch (NullPointerException | ClassNotFoundException e) {
			return null;
		}
	}

	@Override
	public Future<Boolean> send(final MailMessage message) throws MailException {

		final Model related = findEntity(message);
		final MailSender sender = getMailSender(message, related);
		if (sender == null) {
			return new FalseFuture();
		}

		return executor.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				send(sender, message);
				return true;
			}
		});
	}

	/**
	 * Send an email for the given message using the given mail sender.
	 *
	 * <p>
	 * This method runs under a transaction with super user access. It calls
	 * {@link #messageSent(MimeMessage, MailMessage)} after email is
	 * successfully sent. If that method returns updated {@link MailMessage}, it
	 * will persist the updated {@link MailMessage} record.
	 * </p>
	 *
	 * @param store
	 *            the mail store to fetch message from
	 * @throws MailException
	 */
	@Transactional(rollbackOn = Exception.class)
	protected void send(final MailSender sender, final MailMessage message) throws MessagingException, IOException {
		Preconditions.checkNotNull(sender, "mail sender can't be null");
		Preconditions.checkNotNull(message, "mail message can't be null");

		final Model related = findEntity(message);
		final Set<String> recipients = recipients(message, related);
		if (recipients.isEmpty()) {
			return;
		}

		final MailMessageRepository messages = Beans.get(MailMessageRepository.class);
		final MailBuilder builder = sender.compose().subject(getSubject(message, related));

		for (String recipient : recipients) {
			builder.to(recipient);
		}

		builder.html(template(message, related));

		for (MetaAttachment attachment : messages.findAttachments(message)) {
			final Path filePath = MetaFiles.getPath(attachment.getMetaFile());
			final File file = filePath.toFile();
			builder.attach(file.getName(), file.toString());
		}

		final AuditableRunner runner = Beans.get(AuditableRunner.class);
		try {
			runner.run(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					final MimeMessage email = builder.build(message.getMessageId());
					final List<String> references = new ArrayList<>();
					if (message.getParent() != null) {
						references.add(message.getParent().getMessageId());
					}
					if (message.getRoot() != null) {
						references.add(message.getRoot().getMessageId());
					}
					if (!references.isEmpty()) {
						email.setHeader("X-References", Joiner.on(" ").skipNulls().join(references));
					}

					sender.send(email);
					final MailMessage updated = messageSent(email, message);
					if (updated != null && updated.getId() != null) {
						messages.save(updated);
					}
					return true;
				}
			});
		} catch (Exception e) {
			throw new MessagingException("Unable to send email", e);
		}
	}

	/**
	 * This method is called when an email message is sent for the given
	 * message.
	 *
	 * <p>
	 * This method called by {@link #send(MailSender, MailMessage)} which is
	 * running under data transaction. If {@link MailMessage} is not updated,
	 * this method should return null.
	 * </p>
	 *
	 * @param email
	 *            the email message sent
	 * @param message
	 *            the {@link MailMessage} for which the email is sent
	 * @return the update {@link MailMessage} instance or null if not updated
	 * @throws MessagingException
	 * @throws {@link IOException}
	 */
	protected MailMessage messageSent(MimeMessage email, MailMessage message) throws MessagingException, IOException {
		return null;
	}

	/**
	 * This method is called when a new email message received.
	 *
	 * <p>
	 * The method is called by {@link #fetch(Store)} which is running under
	 * database transaction. If this method returns an instance of
	 * {@link MailMessage}, the {@link #fetch(Store)} will persist the record.
	 * Return null ignore the message.
	 * </p>
	 *
	 * <p>
	 * The default implementation will parse the message and check if it's a
	 * reply to a message sent by the system.
	 * </p>
	 *
	 * @param email
	 *            the incoming email message
	 * @return an instance of {@link final MailMessage} to save in database or
	 *         null to ignore
	 * @throws MessagingException
	 *             if there is any error accessing the given message
	 * @throws IOException
	 *             if there is any error accessing message content
	 */
	protected MailMessage messageReceived(MimeMessage email) throws MessagingException, IOException {

		log.info("new email recieved: {}", email.getMessageID());

		final MailParser parser = new MailParser(email).parse();
		final String messageId = email.getMessageID();

		final Set<String> parentIds = new HashSet<>();
		String parentId;

		parentId = parser.getHeader("In-Reply-To");
		if (parentId != null) {
			parentIds.add(parentId);
		}
		parentId = parser.getHeader("X-References");
		if (parentId != null) {
			parentIds.addAll(Splitter.on(" ").trimResults().omitEmptyStrings().splitToList(parentId));
		}

		parentId = parser.getHeader("References");
		if (parentId != null) {
			parentIds.addAll(Splitter.on(" ").trimResults().omitEmptyStrings().splitToList(parentId));
		}

		// default implementation only supports reply
		if (parentIds.isEmpty()) {
			log.info("it's not a reply, ignoring...");
			return null;
		}

		final UserRepository users = Beans.get(UserRepository.class);
		final MailMessageRepository messages = Beans.get(MailMessageRepository.class);

		final MailMessage parent = messages.all().filter("self.messageId in (:ids)").bind("ids", parentIds).fetchOne();

		// no parent message found, ignore
		if (parent == null) {
			log.info("parent message doesn't exist, ignoring...");
			return null;
		}

		final MailMessage existing = messages.all().filter("self.messageId = ?", messageId).fetchOne();

		// very unlikely, message already exist
		if (existing != null) {
			log.info("message already imported, ignoring...");
			return null;
		}

		final MailMessage message = new MailMessage();
		final String content = parser.getHtml() == null ? parser.getText() : parser.getHtml();

		message.setSubject(parser.getSubject());
		message.setBody(content);
		message.setRelatedModel(parent.getRelatedModel());
		message.setRelatedId(parent.getRelatedId());
		message.setRelatedName(parent.getRelatedName());

		final String from = parser.getFrom().getAddress();
		final User author = users.all().filter("self.email = ?", from).fetchOne();

		message.setAuthor(author);

		// need to save before attaching files
		messages.save(message);

		log.info("message created with author: {}", author == null ? from : author);

		// handle attachments
		final MetaAttachmentRepository attachments = Beans.get(MetaAttachmentRepository.class);
		final MetaFiles files = Beans.get(MetaFiles.class);
		for (DataSource ds : parser.getAttachments()) {
			log.info("attaching file: {}", ds.getName());
			final MetaFile file = files.upload(ds.getInputStream(), ds.getName());
			final MetaAttachment attachment = files.attach(file, message);
			attachments.save(attachment);
		}

		return message;
	}

	/**
	 * Fetch email messages from the given mail store.
	 *
	 * @param store
	 *            the mail store to fetch message from
	 * @throws MessagingException
	 *             if unable to parse message
	 * @throws IOException
	 *             if unable to load message content
	 */
	@Transactional(rollbackOn = Exception.class)
	protected void fetch(final MailReader reader) throws MessagingException, IOException {

		final Store store = reader.getStore();
		final Folder inbox = store.getFolder("INBOX");
		final MailMessageRepository repo = Beans.get(MailMessageRepository.class);

		log.debug("Fetching new emails from: {}", store.getURLName());

		// open as READ_WRITE to mark messages as seen
		inbox.open(Folder.READ_WRITE);

		// find all unseen messages
		final FlagTerm unseen = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
		final FetchProfile profile = new FetchProfile();
		final Message[] messages = inbox.search(unseen);

		profile.add(FetchProfile.Item.ENVELOPE);

		// actually fetch the messages
		inbox.fetch(messages, profile);

		int count = 0;
		for (Message message : messages) {
			if (message instanceof MimeMessage) {
				final MailMessage entity = messageReceived((MimeMessage) message);
				if (entity != null) {
					repo.save(entity);
					count += 1;
				}
			}
		}

		log.debug("Fetched {} emails from: {}", count, store.getURLName());
	}

	@Override
	public Future<Boolean> fetch() throws MailException {
		final MailReader reader = getMailReader();
		if (reader == null) {
			return new FalseFuture();
		}
		return executor.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				final boolean[] result = { true };
				final AuditableRunner runner = Beans.get(AuditableRunner.class);
				runner.run(new Runnable() {
					@Override
					public void run() {
						try {
							fetch(reader);
						} catch (Exception e) {
							result[0] = false;
						}
					}
				});
				return result[0];
			}
		});
	}

	@Override
	public List<InternetAddress> findEmails(String matching, List<String> selected, int maxResult) {

		final List<String> where = new ArrayList<>();
		final Map<String, Object> params = new HashMap<>();

		where.add("self.email is not null");

		if (!isBlank(matching)) {
			where.add("(LOWER(self.email) like LOWER(:email) OR LOWER(self.name) like LOWER(:email))");
			params.put("email", "%" + matching + "%");
		}
		if (selected != null && !selected.isEmpty()) {
			where.add("self.email not in (:selected)");
			params.put("selected", selected);
		}

		final String filter = Joiner.on(" AND ").join(where);
		final Query<User> q  = Query.of(User.class);

		if (!isBlank(filter)) {
			q.filter(filter);
			q.bind(params);
		}

		final List<InternetAddress> addresses = new ArrayList<>();

		for (User user : q.fetch(maxResult)) {
			try {
				final InternetAddress item = new InternetAddress(user.getEmail(), user.getName());
				addresses.add(item);
			} catch (UnsupportedEncodingException e) {
			}
		}

		return addresses;
	}
}
