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
package com.axelor.mail.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.TypedQuery;

import com.axelor.auth.AuthUtils;
import com.axelor.db.JpaSupport;
import com.axelor.db.QueryBinder;
import com.axelor.i18n.I18n;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;

public class MailController extends JpaSupport {
	
	private static final String SQL_INBOX = ""
			+ "SELECT DISTINCT(m) FROM MailMessage m LEFT JOIN m.flags g "
			+ "WHERE (m.parent IS NULL) AND "
			+ "((m.createdBy.id = :uid) OR CONCAT(m.relatedId, m.relatedModel) IN "
			+ " (SELECT CONCAT(f.relatedId, f.relatedModel) FROM MailFollower f WHERE f.user.id = :uid)) AND "
			+ "((g IS NULL) OR (g.user.id = :uid AND g.isRead = false)) "
			+ "ORDER BY m.createdOn ASC";

	private static final String SQL_IMPORTANT = ""
			+ "SELECT DISTINCT(m) FROM MailMessage m LEFT JOIN m.flags g "
			+ "WHERE (m.parent IS NULL) AND "
			+ "((m.createdBy.id = :uid) OR CONCAT(m.relatedId, m.relatedModel) IN "
			+ " (SELECT CONCAT(f.relatedId, f.relatedModel) FROM MailFollower f WHERE f.user.id = :uid)) AND "
			+ "((g.user.id = :uid AND g.isStarred = true)) "
			+ "ORDER BY m.createdOn ASC";

	private static final String SQL_ARCHIVE = ""
			+ "SELECT DISTINCT(m) FROM MailMessage m LEFT JOIN m.flags g "
			+ "WHERE (m.parent IS NULL) AND "
			+ "((m.createdBy.id = :uid) OR CONCAT(m.relatedId, m.relatedModel) IN "
			+ " (SELECT CONCAT(f.relatedId, f.relatedModel) FROM MailFollower f WHERE f.user.id = :uid)) AND "
			+ "((g.user.id = :uid AND g.isRead = true)) "
			+ "ORDER BY m.createdOn ASC";

	@Inject
	private MailMessageRepository messages;

	public void unread(ActionRequest request, ActionResponse response) {
		 response.setValue("unread", countUnread());
		 response.setStatus(Response.STATUS_SUCCESS);
	}

	public void inbox(ActionRequest request, ActionResponse response) {

		final List<Object> all = find(SQL_INBOX, request.getLimit());

		response.setValue("$force", true);
		response.setValue("$messages", all);

		if (all.isEmpty()) {
			response.setValue("__emptyTitle", I18n.get("Inbox is empty!"));
			response.setValue("__emptyDesc", I18n.get("Come back later. There are no messages in this folder..."));
		}
	}

	public void important(ActionRequest request, ActionResponse response) {

		final List<Object> all = find(SQL_IMPORTANT, request.getLimit());

		response.setValue("$force", true);
		response.setValue("$messages", all);

		if (all.isEmpty()) {
			response.setValue("__emptyTitle", I18n.get("No important messages!"));
			response.setValue("__emptyDesc", I18n.get("Come back later. There are no messages in this folder..."));
		}
	}

	public void archived(ActionRequest request, ActionResponse response) {

		final List<Object> all = find(SQL_ARCHIVE, request.getLimit());

		response.setValue("$force", true);
		response.setValue("$messages", all);

		if (all.isEmpty()) {
			response.setValue("__emptyTitle", I18n.get("No archived messages!"));
			response.setValue("__emptyDesc", I18n.get("Come back later. There are no messages in this folder..."));
		}
	}

	private List<MailMessage> findChildren(MailMessage message, int level) {
		final List<MailMessage> all = new ArrayList<>();
		all.add(message);
		if (message.getReplies() == null || level > 2) {
			return all;
		}
		for (MailMessage msg : message.getReplies()) {
			all.addAll(findChildren(msg, level + 1));
		}
		return all;
	}

	public long countUnread() {
		final String SQL_INBOX = ""
				+ "SELECT COUNT(m) FROM MailMessage m LEFT JOIN m.flags g "
				+ "WHERE (m.parent IS NULL) AND "
				+ "((m.createdBy.id = :uid) OR CONCAT(m.relatedId, m.relatedModel) IN "
				+ " (SELECT CONCAT(f.relatedId, f.relatedModel) FROM MailFollower f WHERE f.user.id = :uid)) AND "
				+ "((g IS NULL) OR (g.user.id = :uid AND g.isRead = false))";

		final TypedQuery<Long> query = getEntityManager().createQuery(SQL_INBOX, Long.class);
		QueryBinder.of(query).setCacheable();

		query.setParameter("uid", AuthUtils.getUser().getId());
		try {
			return query.getSingleResult();
		} catch (Exception e) {
		}
		return 0;
	}

	public List<Object> find(String queryString, int limit) {

		final TypedQuery<MailMessage> query = getEntityManager().createQuery(queryString, MailMessage.class);
		final List<MailMessage> found = new ArrayList<>();

		query.setParameter("uid", AuthUtils.getUser().getId());
		query.setMaxResults(limit);

		for (MailMessage message : query.getResultList()) {
			found.addAll(findChildren(message, 0));
		}

		final List<Object> all = new ArrayList<>();
		for (MailMessage message : found) {
			Map<String, Object> details = messages.details(message);
			details.put("subject", details.get("relatedName"));
			details.put("$thread", true);
			all.add(details);
		}

		return all;
	}
}
