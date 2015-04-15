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
package com.axelor.mail.db.repo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axelor.auth.AuthUtils;
import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailFlags;
import com.axelor.mail.db.MailMessage;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaAttachmentRepository;
import com.axelor.rpc.Resource;
import com.google.inject.persist.Transactional;

public class MailMessageRepository extends JpaRepository<MailMessage> {

	public MailMessageRepository() {
		super(MailMessage.class);
	}

	public List<MailMessage> findAll(Model entity) {
		return findAll(entity, -1);
	}

	public List<MailMessage> findAll(Model entity, int limit) {
		return all().filter("self.relatedModel = ? AND self.relatedId = ?",
				entity.getClass().getName(),
				entity.getId())
				.order("-createdOn")
				.fetch(limit);
	}

	@Transactional
	public MailMessage post(Model entity, MailMessage message, List<MetaFile> files) {

		Mapper mapper = Mapper.of(entity.getClass());

		message.setRelatedId(entity.getId());
		message.setRelatedModel(entity.getClass().getName());
		message.setAuthor(AuthUtils.getUser());

		try {
			message.setRelatedName(mapper.getNameField().get(entity).toString());
		} catch (Exception e) {
		}

		message = save(message);

		if (files == null || files.isEmpty()) {
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

		return message;
	}

	public Map<String, Object> details(Model entity) {
		final MailFollowerRepository followers = Beans.get(MailFollowerRepository.class);
		final Map<String, Object> details = new HashMap<>();
		final List<MailMessage> messages = findAll(entity);

		final List<Object> all = new ArrayList<>();

		for (MailMessage message : messages) {
			all.add(details(message));
		}

		details.put("$messages", all);
		details.put("$followers", followers.findFollowers(entity));
		details.put("$following", followers.isFollowing(entity, AuthUtils.getUser()));

		return details;
	}

	public Map<String, Object> details(MailMessage message) {
		final Map<String, Object> details = Resource.toMap(message);
		final List<Object> files = new ArrayList<>();

		final MetaAttachmentRepository repoAttachments = Beans.get(MetaAttachmentRepository.class);
		final MailFlagsRepository repoFlags = Beans.get(MailFlagsRepository.class);

		final MailFlags flags = repoFlags.findBy(message, AuthUtils.getUser());
		final List<MetaAttachment> attachments = repoAttachments.all().filter(
				"self.objectId = ? AND self.objectName = ?",
				message.getId(),
				message.getClass().getName()).fetch();

		for (MetaAttachment attachment : attachments) {
			files.add(Resource.toMapCompact(attachment.getMetaFile()));
		}

		if (flags != null) {
			details.put("$flags", Resource.toMap(flags, "starred", "unread", "voted"));
		}

		details.put("$files", files);
		details.put("$eventType", "comment");
		details.put("$eventText", I18n.get("added comment"));
		details.put("$eventTime", message.getCreatedOn());

		return details;
	}
}
