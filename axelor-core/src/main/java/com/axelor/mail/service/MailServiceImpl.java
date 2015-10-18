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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Singleton;
import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.app.AppSettings;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import com.axelor.mail.MailBuilder;
import com.axelor.mail.MailConstants;
import com.axelor.mail.MailException;
import com.axelor.mail.MailSender;
import com.axelor.mail.SmtpAccount;
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailFollowerRepository;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.text.GroovyTemplates;
import com.axelor.text.Template;
import com.axelor.text.Templates;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;

/**
 * Default {@link MailService} implementation.
 *
 */
@Singleton
public class MailServiceImpl implements MailService, MailConstants {

	private MailSender sender;

	private ExecutorService executor = Executors.newCachedThreadPool();

	private Logger log = LoggerFactory.getLogger(MailService.class);

	private static final String NOTIFICATION = "notification";

	public MailServiceImpl() {

		final AppSettings settings = AppSettings.get();

		final String smtpHost = settings.get(CONFIG_SMTP_HOST);
		final String smtpPort = settings.get(CONFIG_SMTP_PORT);
		final String smtpUser = settings.get(CONFIG_SMTP_USER);
		final String smtpPass = settings.get(CONFIG_SMTP_PASS);
		final String smtpChannel = settings.get(CONFIG_SMTP_CHANNEL);

		final int smtpTimeout = settings.getInt(CONFIG_SMTP_TIMEOUT, DEFAULT_TIMEOUT);
		final int smtpConnectionTimeout = settings.getInt(CONFIG_SMTP_CONNECTION_TIMEOUT, DEFAULT_TIMEOUT);

		if (StringUtils.isBlank(smtpHost)) {
			return;
		}

		final SmtpAccount smtpAccount = new SmtpAccount(smtpHost, smtpPort, smtpUser, smtpPass, smtpChannel);

		smtpAccount.setTimeout(smtpTimeout);
		smtpAccount.setConnectionTimeout(smtpConnectionTimeout);

		this.sender = new MailSender(smtpAccount);
	}

	protected String getSubject(MailMessage message, Model entity) {
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
		if (message.getParent() != null && subject != null) {
			subject = "Re: " + subject;
		}
		return subject;
	}

	protected Set<String> recipients(MailMessage message, Model entity) {
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

	protected String template(MailMessage message, Model entity) throws IOException {

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

	protected final Model findEntity(final MailMessage message) {
		try {
			return (Model) JPA.em().find(Class.forName(message.getRelatedModel()), message.getRelatedId());
		} catch (NullPointerException | ClassNotFoundException e) {
			return null;
		}
	}

	@Override
	public Future<Boolean> send(final MailMessage message) throws MailException {

		if (sender == null) {
			return Futures.immediateCancelledFuture();
		}

		final Model related = findEntity(message);
		final Set<String> recipients = recipients(message, related);

		if (recipients.isEmpty()) {
			return Futures.immediateCancelledFuture();
		}

		final MailMessageRepository messages = Beans.get(MailMessageRepository.class);
		final MailBuilder builder = sender.compose().subject(getSubject(message, related));

		for (String recipient : recipients) {
			builder.to(recipient);
		}

		try {
			builder.html(template(message, related));
		} catch (IOException e) {
			throw new MailException("Error processing mail template", e);
		}

		for (MetaAttachment attachment : messages.findAttachments(message)) {
			final Path filePath = MetaFiles.getPath(attachment.getMetaFile());
			final File file = filePath.toFile();
			builder.attach(file.getName(), file.toString());
		}

		return executor.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				try {
					builder.send();
				} catch (Exception e) {
					log.error("Unable to send email.", e);
					return false;
				}
				return true;
			}
		});
	}

	@Override
	public List<InternetAddress> findEmails(String matching, List<String> selected, int maxResult) {

		final List<String> where = new ArrayList<>();
		final Map<String, Object> params = new HashMap<>();

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
