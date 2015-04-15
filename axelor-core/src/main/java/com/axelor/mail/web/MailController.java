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
import com.axelor.i18n.I18n;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class MailController extends JpaSupport {

	@Inject
	private MailMessageRepository messages;

	public void inbox(ActionRequest request, ActionResponse response) {

		final List<Object> all = find(true, request.getLimit());

		response.setValue("$force", true);
		response.setValue("$messages", all);

		if (all.isEmpty()) {
			response.setValue("$emptyTitle", I18n.get("Inbox is empty!"));
		}

		if (all.isEmpty()) {
			response.setValue("__emptyTitle", I18n.get("Inbox is empty!"));
			response.setValue("__emptyDesc", I18n.get("Come back later. There are no messages in this folder..."));
		}
	}

	public void archived(ActionRequest request, ActionResponse response) {

		final List<Object> all = find(false, request.getLimit());

		response.setValue("$force", true);
		response.setValue("$messages", all);

		if (all.isEmpty()) {
			response.setValue("__emptyTitle", I18n.get("There are no archived messages!"));
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

	public List<Object> find(boolean unread, int limit) {

		final String SQL_INBOX = ""
				+ "SELECT DISTINCT(m) FROM MailMessage m, MailFlags g "
				+ "WHERE (m.parent IS NULL) AND "
				+ "((m.createdBy.id = :uid) OR CONCAT(m.relatedId, m.relatedModel) IN "
				+ " (SELECT CONCAT(f.relatedId, f.relatedModel) FROM MailFollower f WHERE f.user.id = :uid)) AND "
				+ "(m.id = g.message.id AND g.unread = true) ORDER BY m.createdOn ASC";

		final String SQL_ARCHIVE = ""
				+ "SELECT DISTINCT(m) FROM MailMessage m "
				+ "WHERE (m.parent IS NULL) AND "
				+ "((m.createdBy.id = :uid) OR CONCAT(m.relatedId, m.relatedModel) IN "
				+ " (SELECT CONCAT(f.relatedId, f.relatedModel) FROM MailFollower f WHERE f.user.id = :uid)) "
				+ "ORDER BY m.createdOn ASC";

		final String queryString = unread ? SQL_INBOX : SQL_ARCHIVE;
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
