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
package com.axelor.mail.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.TypedQuery;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.JpaSupport;
import com.axelor.db.Model;
import com.axelor.db.QueryBinder;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailFlags;
import com.axelor.mail.db.MailGroup;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailFlagsRepository;
import com.axelor.mail.db.repo.MailFollowerRepository;
import com.axelor.mail.db.repo.MailGroupRepository;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.axelor.rpc.Response;

public class MailController extends JpaSupport {
	
	private static final String SQL_UNREAD = ""
			+ "SELECT COUNT(DISTINCT m) FROM MailMessage m LEFT JOIN m.flags g "
			+ "WHERE (CONCAT(m.relatedId, m.relatedModel) IN "
			+ " (SELECT CONCAT(f.relatedId, f.relatedModel) FROM MailFollower f WHERE f.user.id = :uid AND f.archived = false)) AND "
			+ "((g IS NULL) OR (g.user.id != :uid) OR (g.user.id = :uid AND g.isRead = false))";

	private static final String SQL_SUBSCRIBERS = ""
			+ "SELECT DISTINCT(u) FROM User u LEFT JOIN u.group g WHERE "
			+ "(u.id NOT IN (SELECT fu.id FROM MailFollower f LEFT JOIN f.user fu WHERE f.relatedId = :id AND f.relatedModel = :model)) AND "
			+ "((u.id IN (SELECT mu.id FROM MailGroup m LEFT JOIN m.users mu WHERE m.id = :id)) OR "
			+ "	(g.id IN (SELECT mg.id FROM MailGroup m LEFT JOIN m.groups mg WHERE m.id = :id)))";

	private static final String SQL_INBOX = ""
			+ "SELECT DISTINCT(m) FROM MailMessage m LEFT JOIN m.flags g "
			+ "WHERE (m.parent IS NULL) AND "
			+ "(CONCAT(m.relatedId, m.relatedModel) IN "
			+ " (SELECT CONCAT(f.relatedId, f.relatedModel) FROM MailFollower f WHERE f.user.id = :uid AND f.archived = false)) AND "
			+ "((g IS NULL) OR (g.user.id != :uid) OR (g.user.id = :uid AND (g.isRead = false OR g.isArchived = false))) "
			+ "ORDER BY m.createdOn DESC";

	private static final String SQL_IMPORTANT = ""
			+ "SELECT DISTINCT(m) FROM MailMessage m LEFT JOIN m.flags g "
			+ "WHERE (m.parent IS NULL) AND "
			+ "(CONCAT(m.relatedId, m.relatedModel) IN "
			+ " (SELECT CONCAT(f.relatedId, f.relatedModel) FROM MailFollower f WHERE f.user.id = :uid AND f.archived = false)) AND "
			+ "((g.user.id = :uid AND g.isStarred = true AND g.isArchived = false)) "
			+ "ORDER BY m.createdOn DESC";

	private static final String SQL_ARCHIVE = ""
			+ "SELECT DISTINCT(m) FROM MailMessage m LEFT JOIN m.flags g "
			+ "WHERE (m.parent IS NULL) AND "
			+ "(CONCAT(m.relatedId, m.relatedModel) IN "
			+ " (SELECT CONCAT(f.relatedId, f.relatedModel) FROM MailFollower f WHERE f.user.id = :uid AND f.archived = false)) AND "
			+ "((g.user.id = :uid AND g.isArchived = true)) "
			+ "ORDER BY m.createdOn DESC";

	@Inject
	private MailMessageRepository messages;

	public void unread(ActionRequest request, ActionResponse response) {
		response.setValue("unread", countUnread());
		response.setStatus(Response.STATUS_SUCCESS);
	}

	public void inbox(ActionRequest request, ActionResponse response) {

		final List<Object> all = find(SQL_INBOX, request.getOffset(), request.getLimit());
		final Long total = count(SQL_INBOX);

		response.setData(all);
		response.setOffset(request.getOffset());
		response.setTotal(total);
	}

	public void important(ActionRequest request, ActionResponse response) {

		final List<Object> all = find(SQL_IMPORTANT, request.getOffset(), request.getLimit());
		final Long total = count(SQL_IMPORTANT);

		response.setData(all);
		response.setOffset(request.getOffset());
		response.setTotal(total);
	}

	public void archived(ActionRequest request, ActionResponse response) {

		final List<Object> all = find(SQL_ARCHIVE, request.getOffset(), request.getLimit());
		final Long total = count(SQL_ARCHIVE);

		response.setData(all);
		response.setOffset(request.getOffset());
		response.setTotal(total);
	}

	public void related(ActionRequest request, ActionResponse response) {

		if (request.getRecords() == null ||
			request.getRecords().isEmpty()) {
			return;
		}

		final Model related = (Model) request.getRecords().get(0);
		final List<MailMessage> all = messages.findRelated(related, request.getLimit(), request.getOffset());
		final Long count = messages.countRelated(related);

		final List<Object> data = new ArrayList<>();
		for (MailMessage message : all) {
			data.add(messages.details(message));
		}

		response.setData(data);
		response.setOffset(request.getOffset());
		response.setTotal(count);
	}

	public void replies(ActionRequest request, ActionResponse response) {

		if (request.getRecords() == null ||
			request.getRecords().isEmpty()) {
			return;
		}

		final MailMessage parent = messages.find((Long) request.getRecords().get(0));
		final List<MailMessage> found = findChildren(parent);
		final List<Object> all = new ArrayList<>();

		for (MailMessage message : found) {
			Map<String, Object> details = messages.details(message);
			details.put("$thread", true);
			all.add(details);
		}

		response.setData(all);
		response.setStatus(ActionResponse.STATUS_SUCCESS);
	}

	public void autoSubscribe(ActionRequest request, ActionResponse response) {

		final MailFollowerRepository followers = Beans.get(MailFollowerRepository.class);
		final MailGroupRepository groups = Beans.get(MailGroupRepository.class);
		final MailGroup group = request.getContext().asType(MailGroup.class);

		if (group == null || group.getId() == null) {
			return;
		}

		final TypedQuery<User> query = getEntityManager().createQuery(SQL_SUBSCRIBERS, User.class);
		query.setParameter("id", group.getId());
		query.setParameter("model", MailGroup.class.getName());

		final List<User> users = query.getResultList();
		final MailGroup entity = groups.find(group.getId());
		for (User user : users) {
			followers.follow(entity, user);
		}

		response.setStatus(ActionResponse.STATUS_SUCCESS);
	}

	public void follow(ActionRequest request, ActionResponse response) {
		final Context ctx = request.getContext();
		final Long id = (Long) ctx.get("id");
		final Model entity = (Model) getEntityManager().find(ctx.getContextClass(), id);
		final MailFollowerRepository followers = Beans.get(MailFollowerRepository.class);

		followers.follow(entity, AuthUtils.getUser());

		response.setValue("_following", true);
		response.setStatus(ActionResponse.STATUS_SUCCESS);
	}

	public void unfollow(ActionRequest request, ActionResponse response) {
		final Context ctx = request.getContext();
		final Long id = (Long) ctx.get("id");
		final Model entity = (Model) getEntityManager().find(ctx.getContextClass(), id);
		final MailFollowerRepository followers = Beans.get(MailFollowerRepository.class);

		followers.unfollow(entity, AuthUtils.getUser());

		response.setValue("_following", false);
		response.setStatus(ActionResponse.STATUS_SUCCESS);
	}

	public String inboxMenuTag() {
		Long total = count(SQL_INBOX);
		Long unread = countUnread();
		if (total == null) {
			return null;
		}
		if (unread == null) {
			unread = 0L;
		}
		return String.format("%s/%s", unread, total);
	}

	private List<MailMessage> findChildren(MailMessage message) {
		final List<MailMessage> all = new ArrayList<>();
		if (message.getReplies() == null) {
			return all;
		}
		for (MailMessage msg : message.getReplies()) {
			all.add(msg);
			all.addAll(findChildren(msg));
		}
		return all;
	}

	private Long countUnread() {
		final TypedQuery<Long> query = getEntityManager().createQuery(SQL_UNREAD, Long.class);
		QueryBinder.of(query).setCacheable();

		query.setParameter("uid", AuthUtils.getUser().getId());
		try {
			return query.getSingleResult();
		} catch (Exception e) {
		}
		return 0L;
	}

	private Long count(String queryString) {

		final String countString = queryString
				.replace("DISTINCT(m)", "COUNT(DISTINCT m)")
				.replace(" ORDER BY m.createdOn DESC", "");

		final TypedQuery<Long> query = getEntityManager().createQuery(countString, Long.class);

		query.setParameter("uid", AuthUtils.getUser().getId());

		try {
			return query.getSingleResult();
		} catch (Exception e) {
		}
		return 0L;
	}

	private List<Object> find(String queryString, int offset, int limit) {

		final TypedQuery<MailMessage> query = getEntityManager().createQuery(queryString, MailMessage.class);
		final MailFlagsRepository flagsRepo = Beans.get(MailFlagsRepository.class);

		query.setParameter("uid", AuthUtils.getUser().getId());

		if (offset > 0) query.setFirstResult(offset);
		if (limit > 0) query.setMaxResults(limit);

		final List<MailMessage> found = query.getResultList();
		final List<Object> all = new ArrayList<>();

		for (MailMessage message : found) {
			final Map<String, Object> details = messages.details(message);
			final List<MailMessage> replies = messages.all()
					.filter("self.root.id = ?", message.getId())
					.order("-createdOn").fetch();
			final List<Object> unread = new ArrayList<>();

			for (MailMessage reply : replies) {
				MailFlags flags = flagsRepo.findBy(reply, AuthUtils.getUser());
				if (flags != null && flags.getIsRead() == Boolean.FALSE) {
					unread.add(messages.details(reply));
				}
			}

			details.put("$name", details.get("relatedName"));
			details.put("$thread", true);
			details.put("$numReplies", replies.size());
			details.put("$children", unread);
			details.put("$hasMore", replies.size() > unread.size());
			all.add(details);
		}

		return all;
	}
}
