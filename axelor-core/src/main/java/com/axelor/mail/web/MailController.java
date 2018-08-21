/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.JpaSupport;
import com.axelor.db.Model;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailFlags;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailFlagsRepository;
import com.axelor.mail.db.repo.MailFollowerRepository;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.axelor.rpc.Response;
import com.axelor.team.db.Team;
import com.axelor.team.db.repo.TeamRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.persistence.TypedQuery;

public class MailController extends JpaSupport {

  private static final String SQL_UNREAD =
      ""
          + "SELECT mm FROM MailMessage mm "
          + "LEFT JOIN MailFollower f ON f.relatedId = mm.relatedId and f.relatedModel = mm.relatedModel "
          + "LEFT JOIN MailFlags g ON g.user = f.user AND g.message = mm.id "
          + "WHERE"
          + " (mm.parent IS NULL) AND "
          + " (f.user.id = :uid AND f.archived = false) AND"
          + " (g.isRead IS NULL OR g.isRead = false) "
          + "ORDER BY mm.createdOn DESC";

  private static final String SQL_SUBSCRIBERS =
      ""
          + "SELECT DISTINCT(u) FROM User u "
          + "LEFT JOIN u.group g "
          + "LEFT JOIN u.roles r "
          + "LEFT JOIN g.roles gr "
          + "WHERE "
          + "(u.id NOT IN (SELECT fu.id FROM MailFollower f LEFT JOIN f.user fu WHERE f.relatedId = :id AND f.relatedModel = :model)) AND "
          + "((u.id IN (SELECT mu.id FROM Team m LEFT JOIN m.members mu WHERE m.id = :id)) OR "
          + "	(r.id IN (SELECT mr.id FROM Team m LEFT JOIN m.roles mr WHERE m.id = :id)) OR "
          + " (gr.id IN (SELECT mr.id FROM Team m LEFT JOIN m.roles mr WHERE m.id = :id)))";

  private static final String SQL_INBOX =
      ""
          + "SELECT mm FROM MailMessage mm "
          + "LEFT JOIN MailFollower f ON f.relatedId = mm.relatedId and f.relatedModel = mm.relatedModel "
          + "LEFT JOIN MailFlags g ON g.user = f.user AND g.message = mm.id "
          + "WHERE"
          + " (mm.parent IS NULL) AND "
          + " (f.user.id = :uid AND f.archived = false) AND"
          + " (g.isRead IS NULL OR g.isRead = false OR g.isArchived = false) "
          + "ORDER BY mm.createdOn DESC";

  private static final String SQL_IMPORTANT =
      ""
          + "SELECT mm FROM MailMessage mm "
          + "LEFT JOIN MailFollower f ON f.relatedId = mm.relatedId and f.relatedModel = mm.relatedModel "
          + "LEFT JOIN MailFlags g ON g.user = f.user AND g.message = mm.id "
          + "WHERE"
          + " (mm.parent IS NULL) AND "
          + " (f.user.id = :uid AND f.archived = false) AND"
          + " (g.isStarred = true AND g.isArchived = false) "
          + "ORDER BY mm.createdOn DESC";

  private static final String SQL_ARCHIVE =
      ""
          + "SELECT mm FROM MailMessage mm "
          + "LEFT JOIN MailFollower f ON f.relatedId = mm.relatedId and f.relatedModel = mm.relatedModel "
          + "LEFT JOIN MailFlags g ON g.user = f.user AND g.message = mm.id "
          + "WHERE"
          + " (mm.parent IS NULL) AND "
          + " (f.user.id = :uid AND f.archived = false) AND"
          + " (g.isArchived = true) "
          + "ORDER BY mm.createdOn DESC";

  @Inject private MailMessageRepository messages;

  public void countMail(ActionRequest request, ActionResponse response) {
    final Map<String, Object> value = new HashMap<>();
    value.put("total", countMessages(SQL_INBOX));
    value.put("unread", countMessages(SQL_UNREAD));
    response.setValue("mail", value);
    response.setStatus(Response.STATUS_SUCCESS);
  }

  public void countUnread(ActionRequest request, ActionResponse response) {
    response.setValue("unread", countMessages(SQL_UNREAD));
    response.setStatus(Response.STATUS_SUCCESS);
  }

  public void unread(ActionRequest request, ActionResponse response) {

    final List<Object> all = find(SQL_UNREAD, request.getOffset(), request.getLimit());
    final Long total = countMessages(SQL_UNREAD);

    response.setData(all);
    response.setOffset(request.getOffset());
    response.setTotal(total);
  }

  public void inbox(ActionRequest request, ActionResponse response) {

    final List<Object> all = find(SQL_INBOX, request.getOffset(), request.getLimit());
    final Long total = countMessages(SQL_INBOX);

    response.setData(all);
    response.setOffset(request.getOffset());
    response.setTotal(total);
  }

  public void important(ActionRequest request, ActionResponse response) {

    final List<Object> all = find(SQL_IMPORTANT, request.getOffset(), request.getLimit());
    final Long total = countMessages(SQL_IMPORTANT);

    response.setData(all);
    response.setOffset(request.getOffset());
    response.setTotal(total);
  }

  public void archived(ActionRequest request, ActionResponse response) {

    final List<Object> all = find(SQL_ARCHIVE, request.getOffset(), request.getLimit());
    final Long total = countMessages(SQL_ARCHIVE);

    response.setData(all);
    response.setOffset(request.getOffset());
    response.setTotal(total);
  }

  public void related(ActionRequest request, ActionResponse response) {

    if (request.getRecords() == null || request.getRecords().isEmpty()) {
      return;
    }

    final Model related = (Model) request.getRecords().get(0);
    final List<MailMessage> all =
        messages.findAll(related, request.getLimit(), request.getOffset());
    final Long count = messages.count(related);

    final List<Object> data = new ArrayList<>();
    for (MailMessage message : all) {
      data.add(messages.details(message));
    }

    response.setData(data);
    response.setOffset(request.getOffset());
    response.setTotal(count);
  }

  public void replies(ActionRequest request, ActionResponse response) {

    if (request.getRecords() == null || request.getRecords().isEmpty()) {
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
    final TeamRepository teams = Beans.get(TeamRepository.class);
    final Team team = request.getContext().asType(Team.class);

    if (team == null || team.getId() == null) {
      return;
    }

    final TypedQuery<User> query = getEntityManager().createQuery(SQL_SUBSCRIBERS, User.class);
    query.setParameter("id", team.getId());
    query.setParameter("model", Team.class.getName());

    final List<User> users = query.getResultList();
    final Team entity = teams.find(team.getId());
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
    Long total = countMessages(SQL_INBOX);
    Long unread = countMessages(SQL_UNREAD);
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
    for (MailMessage msg :
        messages.all().filter("self.parent.id = ?", message.getId()).order("-createdOn").fetch()) {
      all.add(msg);
      all.addAll(findChildren(msg));
    }
    return all;
  }

  private Long countMessages(String queryString) {

    final String countString =
        queryString
            .replace("SELECT mm FROM MailMessage mm", "SELECT COUNT(mm.id) FROM MailMessage mm")
            .replace(" ORDER BY mm.createdOn DESC", "");

    final TypedQuery<Long> query = getEntityManager().createQuery(countString, Long.class);

    query.setParameter("uid", AuthUtils.getUser().getId());

    try {
      return query.getSingleResult();
    } catch (Exception e) {
    }
    return 0L;
  }

  private List<Object> find(String queryString, int offset, int limit) {

    final TypedQuery<MailMessage> query =
        getEntityManager().createQuery(queryString, MailMessage.class);
    final MailFlagsRepository flagsRepo = Beans.get(MailFlagsRepository.class);

    query.setParameter("uid", AuthUtils.getUser().getId());

    if (offset > 0) query.setFirstResult(offset);
    if (limit > 0) query.setMaxResults(limit);

    final List<MailMessage> found = query.getResultList();
    final List<Object> all = new ArrayList<>();

    for (MailMessage message : found) {
      final Map<String, Object> details = messages.details(message);
      final List<MailMessage> replies =
          messages.all().filter("self.root.id = ?", message.getId()).order("-createdOn").fetch();
      final List<Object> unread = new ArrayList<>();

      for (MailMessage reply : replies) {
        final MailFlags flags = flagsRepo.findBy(reply, AuthUtils.getUser());
        if (flags == null || flags.getIsRead() == Boolean.FALSE) {
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
