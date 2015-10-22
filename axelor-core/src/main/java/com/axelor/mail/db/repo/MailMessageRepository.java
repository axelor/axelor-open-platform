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
package com.axelor.mail.db.repo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.mail.internet.InternetAddress;
import javax.persistence.PersistenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.EntityHelper;
import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.mail.MailConstants;
import com.axelor.mail.MailException;
import com.axelor.mail.db.MailAddress;
import com.axelor.mail.db.MailFlags;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.service.MailService;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaActionRepository;
import com.axelor.meta.db.repo.MetaAttachmentRepository;
import com.axelor.rpc.Resource;
import com.google.inject.persist.Transactional;

public class MailMessageRepository extends JpaRepository<MailMessage> {

	@Inject
	private MetaFiles files;

	@Inject
	private MailService mailService;

	@Inject
	private MetaAttachmentRepository attachmentRepo;

	@Inject
	private MetaActionRepository actionRepo;

	private Logger log = LoggerFactory.getLogger(MailMessageRepository.class);

	public MailMessageRepository() {
		super(MailMessage.class);
	}

	public List<MailMessage> findRelated(Model entity, int limit, int offset) {
		return all().filter("self.relatedModel = ? AND self.relatedId = ?",
				EntityHelper.getEntityClass(entity).getName(),
				entity.getId())
				.order("-createdOn")
				.fetch(limit, offset);
	}

	public long countRelated(Model entity) {
		return all().filter("self.relatedModel = ? AND self.relatedId = ?",
				EntityHelper.getEntityClass(entity).getName(),
				entity.getId()).count();
	}

	@Override
	public void remove(MailMessage message) {
		// delete all attachments
		List<MetaAttachment> attachments = attachmentRepo.all()
				.filter("self.objectId = ? AND self.objectName = ?", message.getId(), MailMessage.class.getName())
				.fetch();

		for (MetaAttachment attachment : attachments) {
			try {
				files.delete(attachment);
			} catch (IOException e) {
				throw new PersistenceException(e);
			}
		}
		super.remove(message);
	}

	private static String mailHost;
	private static AtomicInteger mailId = new AtomicInteger();

	protected String generateMessageId(MailMessage entity) {
		if (mailHost == null) {
			final InternetAddress addr = InternetAddress.getLocalAddress(null);
			mailHost = addr == null ? "javamailuser@localhost" : addr.getAddress();
			if (mailHost.indexOf("@") > 0) {
				mailHost = mailHost.substring(mailHost.lastIndexOf("@"));
			}
		}
		final StringBuilder builder = new StringBuilder();
		builder.append("<");
		builder.append(builder.hashCode()).append(".");
		builder.append(mailId.getAndIncrement()).append(".");
		builder.append(System.currentTimeMillis());
		builder.append(mailHost);
		builder.append(">");
		return builder.toString();
	}

	@Override
	public MailMessage save(MailMessage entity) {
		if (entity.getParent() == null && entity.getRelatedId() != null) {
			MailMessage parent = all()
				.filter("self.parent is null AND "
						+ "self.type = :type AND "
						+ "self.relatedId = :id AND self.relatedModel = :model")
				.bind("id", entity.getRelatedId())
				.bind("model", entity.getRelatedModel())
				.bind("type", MailConstants.MESSAGE_TYPE_NOTIFICATION)
				.order("id")
				.cacheable()
				.autoFlush(false)
				.fetchOne();
			entity.setParent(parent);
		}

		MailMessage root = entity.getRoot();
		if (root == null) {
			root = entity.getParent();
		}
		if (root != null && root.getRoot() != null) {
			root = root.getRoot();
		}
		entity.setRoot(root);

		// mark root as unread
		if (root != null && root.getFlags() != null && entity.getFlags() == null) {
			for (MailFlags rootFlags : root.getFlags()) {
				rootFlags.setIsRead(false);
			}
		}

		boolean isNew = entity.getId() == null;
		boolean isNotification = MailConstants.MESSAGE_TYPE_NOTIFICATION.equals(entity.getType());

		// make sure to set unique message-id
		if (entity.getMessageId() == null) {
			entity.setMessageId(generateMessageId(entity));
		}

		final MailMessage saved = super.save(entity);

		// notify all followers by email
		if (isNotification && isNew) {
			email(saved);
		}

		return saved;
	}

	public void email(MailMessage message) {
		try {
			mailService.send(message);
		} catch (MailException e) {
			log.error("Error sending mail: " + e.getMessage(), e);
		}
	}

	@Transactional
	public MailMessage post(Model entity, MailMessage message, List<MetaFile> files) {

		final Mapper mapper = Mapper.of(EntityHelper.getEntityClass(entity));
		final MailFlags  flags = new MailFlags();

		message.setRelatedId(entity.getId());
		message.setRelatedModel(EntityHelper.getEntityClass(entity).getName());
		message.setAuthor(AuthUtils.getUser());

		// mark message as read
		flags.setMessage(message);
		flags.setUser(AuthUtils.getUser());
		flags.setIsRead(Boolean.TRUE);
		message.addFlag(flags);

		try {
			message.setRelatedName(mapper.getNameField().get(entity).toString());
		} catch (Exception e) {
		}

		if (message.getRecipients() != null) {
			final Set<MailAddress> recipients = new HashSet<>();
			final MailAddressRepository addresses = Beans.get(MailAddressRepository.class);
			for (MailAddress address : message.getRecipients()) {
				recipients.add(addresses.findOrCreate(address.getAddress(), address.getPersonal()));
			}
			message.clearRecipients();
			message.setRecipients(recipients);
		}

		message = save(message);

		if (files == null || files.isEmpty()) {
			// notify all followers by email
			email(message);
			return message;
		}

		final MetaAttachmentRepository repo = Beans.get(MetaAttachmentRepository.class);

		for (MetaFile file : files) {

			MetaAttachment attachment = new MetaAttachment();

			attachment.setObjectId(message.getId());
			attachment.setObjectName(message.getClass().getName());
			attachment.setMetaFile(file);

			repo.save(attachment);
		}

		// notify all followers by email
		email(message);

		return message;
	}

	public List<MetaAttachment> findAttachments(MailMessage message) {
		final MetaAttachmentRepository repoAttachments = Beans.get(MetaAttachmentRepository.class);
		return repoAttachments.all().filter(
				"self.objectId = ? AND self.objectName = ?",
				message.getId(),
				EntityHelper.getEntityClass(message).getName()).fetch();
	}

	public Map<String, Object> details(MailMessage message) {
		final String[] fields = { "id", "type", "subject", "body", "summary", "relatedId", "relatedModel", "relatedName" };
		final Map<String, Object> details = Resource.toMap(message, fields);
		final List<Object> files = new ArrayList<>();

		final MailService mailService = Beans.get(MailService.class);
		final MailFlagsRepository repoFlags = Beans.get(MailFlagsRepository.class);

		final MailFlags flags = repoFlags.findBy(message, AuthUtils.getUser());
		final List<MetaAttachment> attachments = findAttachments(message);

		for (MetaAttachment attachment : attachments) {
			files.add(Resource.toMapCompact(attachment.getMetaFile()));
		}

		if (flags != null) {
			details.put("$flags", Resource.toMap(flags, "isRead", "isStarred", "isArchived"));
		}

		String eventType = message.getType();
		String eventText = I18n.get("updated document");
		if (MailConstants.MESSAGE_TYPE_COMMENT.equals(eventType) ||
			MailConstants.MESSAGE_TYPE_EMAIL.equals(eventType)) {
			eventText = I18n.get("added comment");
			details.put("$canDelete", message.getCreatedBy() == AuthUtils.getUser());
		}

		final MailAddress email = message.getFrom();
		final User user = message.getAuthor();

		Model author = user;
		if (author == null && email != null) {
			author = mailService.resolve(email.getAddress());
		}

		if (author != null) {
			final String authorModel = EntityHelper.getEntityClass(author).getName();
			final MetaAction authorAction = actionRepo.all()
					.filter("self.type = 'action-view' and self.model = ?", authorModel).fetchOne();
			if (authorAction != null) {
				details.put("$authorAction", authorAction.getName());
			}
			details.put("$authorModel", authorModel);
		}

		String avatar = "img/user.png";
		if (user != null && user.getImage() != null) {
			avatar = "ws/rest/" + User.class.getName() + "/" + user.getId() + "/image/download?image=true&v=" + user.getVersion();
		}

		details.put("$avatar", avatar);
		details.put("$from", Resource.toMap(email, "address", "personal"));
		details.put("$author", author);
		details.put("$files", files);
		details.put("$eventType", eventType);
		details.put("$eventText", eventText);
		details.put("$eventTime", message.getCreatedOn());

		return details;
	}
}
