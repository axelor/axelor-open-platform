/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.web.service;

import com.axelor.auth.AuthSecurityException;
import com.axelor.auth.AuthUtils;
import com.axelor.common.ObjectUtils;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.db.mapper.Adapter;
import com.axelor.inject.Beans;
import com.axelor.mail.MailConstants;
import com.axelor.mail.db.MailFlags;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.axelor.mail.web.MailController;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.inject.persist.Transactional;
import com.google.inject.servlet.RequestScoped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.shiro.authz.UnauthorizedException;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/messages")
public class MessageService extends AbstractService {

  /**
   * Give message details for the given id
   *
   * @param id the message id
   * @return response
   */
  @GET
  @Path("{id}")
  public Response getMessage(@PathParam("id") long id) {
    final ActionResponse res = new ActionResponse();
    final MailMessage message = JPA.edit(MailMessage.class, ImmutableMap.of("id", id));

    if (message == null) {
      return res;
    }

    try {
      checkMailMessagePerms(message);
    } catch (ClassNotFoundException e) {
      res.setStatus(Response.STATUS_FAILURE);
      return res;
    }

    res.setData(Beans.get(MailMessageRepository.class).details(message));
    res.setStatus(Response.STATUS_SUCCESS);
    return res;
  }

  /**
   * Get all messages for the given folder or an entity identified by the given relatedId and
   * relatedModel.
   *
   * @param folder the folder
   * @param relatedId the relatedId
   * @param relatedModel the relatedModel
   * @param limit the limit
   * @param offset the offset
   * @return response
   */
  @GET
  public Response messages(
      @QueryParam("folder") @DefaultValue("inbox") String folder,
      @QueryParam("relatedId") Long relatedId,
      @QueryParam("relatedModel") String relatedModel,
      @QueryParam("type") String type,
      @QueryParam("limit") @DefaultValue("10") Integer limit,
      @QueryParam("offset") @DefaultValue("0") Integer offset) {

    final ActionRequest req = new ActionRequest();

    req.setModel(relatedModel);
    req.setOffset(offset);
    req.setLimit(limit);

    if (type != null) {
      req.setData(Map.of("context", Map.of("type", type)));
    }

    if (relatedId != null && relatedModel != null) {
      return findByRelated(req, relatedId, relatedModel);
    }

    return findByFolder(req, folder);
  }

  /**
   * Get all message for an entity identified by the given relatedId and relatedModel.
   *
   * @param req ActionRequest
   * @param relatedId the relatedId
   * @param relatedModel the relatedModel
   * @return response
   */
  private Response findByRelated(ActionRequest req, Long relatedId, String relatedModel) {
    final ActionResponse res = new ActionResponse();
    Class<?> relatedClass;

    try {
      relatedClass = Class.forName(relatedModel);
      checkMailMessagePerms(relatedModel, relatedId);
    } catch (ClassNotFoundException e) {
      res.setStatus(Response.STATUS_FAILURE);
      return res;
    }

    Model related = JpaRepository.of(relatedClass.asSubclass(Model.class)).find(relatedId);

    if (related != null) {
      final List<Object> records = new ArrayList<>();
      records.add(related);
      req.setRecords(records);
      Beans.get(MailController.class).related(req, res);
      return res;
    }

    return res;
  }

  /**
   * Get all message for the given folder
   *
   * @param req ActionRequest
   * @param folder the folder
   * @return response
   */
  private Response findByFolder(ActionRequest req, String folder) {
    final ActionResponse res = new ActionResponse();
    final MailController ctrl = Beans.get(MailController.class);

    switch (folder) {
      case "archive":
        ctrl.archived(req, res);
        return res;
      case "important":
        ctrl.important(req, res);
        return res;
      case "unread":
        ctrl.unread(req, res);
        return res;
      default:
        ctrl.inbox(req, res);
        return res;
    }
  }

  /**
   * Get all replied of the message identified with the given id
   *
   * @param id root message id
   * @return response
   */
  @GET
  @Path("{id}/replies")
  public Response replies(@PathParam("id") long id) {
    final ActionRequest req = new ActionRequest();
    final ActionResponse res = new ActionResponse();

    List<Object> records = new ArrayList<>();
    records.add(id);
    req.setRecords(records);
    Beans.get(MailController.class).replies(req, res);

    return res;
  }

  /**
   * Count number of unread message for the current user
   *
   * @return response
   */
  @GET
  @Path("/count")
  public Response count() {
    final ActionRequest req = new ActionRequest();
    final ActionResponse res = new ActionResponse();

    Beans.get(MailController.class).countUnread(req, res);
    return res;
  }

  /**
   * Remove message with the given id
   *
   * @param id message id to remove
   * @return response
   */
  @DELETE
  @Path("{id}")
  public Response messageRemove(@PathParam("id") long id) {
    removeMessage(id);

    final Response response = new Response();
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  /**
   * Remove message with the given id
   *
   * @param id message id to remove
   */
  @Transactional
  public void removeMessage(long id) {
    final MailMessage message = JPA.edit(MailMessage.class, ImmutableMap.of("id", id));
    final String eventType = message.getType();

    if (!MailConstants.MESSAGE_TYPE_COMMENT.equals(eventType)
            && !MailConstants.MESSAGE_TYPE_EMAIL.equals(eventType)
        || !Objects.equal(message.getCreatedBy(), AuthUtils.getUser())) {
      final AuthSecurityException cause =
          new AuthSecurityException(JpaSecurity.AccessType.REMOVE, MailMessage.class, id);
      throw new UnauthorizedException(cause.getMessage(), cause);
    }

    JpaRepository.of(MailMessage.class).remove(message);
  }

  /**
   * Flag message identified with the given id
   *
   * @param id message id to flag
   * @param request flag data
   * @return response
   */
  @POST
  @Path("{id}/flag")
  public Response flagMessage(@PathParam("id") long id, Request request) {
    final ActionResponse res = new ActionResponse();

    if (ObjectUtils.isEmpty(request.getData())) {
      res.setStatus(Response.STATUS_FAILURE);
      return res;
    }

    flagMessages(Collections.singletonList(request.getData()));
    return res;
  }

  /**
   * Flag messages contains in the given request
   *
   * @param request the request contains the flags
   * @return response
   */
  @POST
  @Path("flag")
  public Response flagMessages(Request request) {
    final ActionResponse res = new ActionResponse();

    if (ObjectUtils.isEmpty(request.getRecords())) {
      res.setStatus(Response.STATUS_FAILURE);
      return res;
    }

    final List<Map<String, Object>> data = flagMessages(request.getRecords());
    res.setData(data);
    return res;
  }

  @Transactional
  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> flagMessages(List<Object> records) {
    final List<Map<String, Object>> data = new ArrayList<>(records.size());
    int counting = 0;

    for (Object record : records) {
      final Map<String, Object> map;
      @SuppressWarnings("rawtypes")
      MailFlags flag = flagMessage((Map) record);

      if (flag != null) {
        counting++;
        flag = JpaRepository.of(MailFlags.class).save(flag);
        map = Map.of("id", flag.getId(), "version", flag.getVersion());
      } else {
        map = Collections.emptyMap();
      }

      data.add(map);

      if (counting % 20 == 0) {
        JPA.clear();
      }
    }

    return data;
  }

  private MailFlags flagMessage(Map<String, Object> record) {
    MailFlags flag = getFlag(record.get("id"));
    if (flag == null) {
      flag = new MailFlags();
      flag.setUser(AuthUtils.getUser());
      flag.setMessage(getMessage(record.get("messageId")));
      if (flag.getMessage() == null) {
        return null;
      }
    }

    try {
      checkMailFlagPerms(
          flag.getId() == null ? JpaSecurity.AccessType.CREATE : JpaSecurity.AccessType.WRITE,
          flag);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

    if (record.containsKey("isArchived")) {
      flag.setIsArchived(
          (Boolean) Adapter.adapt(record.get("isArchived"), Boolean.class, null, null));
    }

    if (record.containsKey("isRead")) {
      flag.setIsRead((Boolean) Adapter.adapt(record.get("isRead"), Boolean.class, null, null));
    }

    if (record.containsKey("isStarred")) {
      flag.setIsStarred(
          (Boolean) Adapter.adapt(record.get("isStarred"), Boolean.class, null, null));
    }

    return flag;
  }

  private void checkMailMessagePerms(MailMessage message) throws ClassNotFoundException {
    checkMailMessagePerms(message.getRelatedModel(), message.getRelatedId());
  }

  private void checkMailMessagePerms(String relatedModel, Long relatedId)
      throws ClassNotFoundException {
    Class<?> relatedClass = Class.forName(relatedModel);

    Beans.get(JpaSecurity.class)
        .check(JpaSecurity.CAN_READ, relatedClass.asSubclass(Model.class), relatedId);
  }

  private void checkMailFlagPerms(JpaSecurity.AccessType type, MailFlags flag)
      throws ClassNotFoundException {
    if (type == JpaSecurity.AccessType.CREATE) {
      checkMailMessagePerms(flag.getMessage());
    }

    if (flag.getUser() != AuthUtils.getUser()) {
      final AuthSecurityException cause =
          new AuthSecurityException(type, MailFlags.class, flag.getId());
      throw new UnauthorizedException(cause.getMessage(), cause);
    }
  }

  private MailMessage getMessage(Object id) {
    try {
      return JPA.find(MailMessage.class, Long.parseLong(id.toString()));
    } catch (Exception e) {
      return null;
    }
  }

  private MailFlags getFlag(Object id) {
    try {
      return JPA.find(MailFlags.class, Long.parseLong(id.toString()));
    } catch (Exception e) {
      return null;
    }
  }
}
