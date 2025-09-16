/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.socket.channels;

import com.axelor.mail.web.MailController;
import com.axelor.meta.service.tags.TagsService;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.axelor.team.web.TaskController;
import com.axelor.web.socket.Channel;
import com.axelor.web.socket.Message;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Session;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;

@Singleton
public class TagsChannel extends Channel {

  private static final String NAME = "tags";

  @Inject private Logger log;

  @Inject private TagsService tagsService;

  @Inject private TaskController teamController;

  @Inject private MailController mailController;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  @Transactional
  public void onMessage(Session session, Message message) {
    final ActionResponse response = new ActionResponse();
    final List<String> names = getNames(message);

    response.setValue("tags", tagsService.get(names));
    response.setStatus(Response.STATUS_SUCCESS);
    mailController.countMail(null, response);
    teamController.countTasks(null, response);

    try {
      this.send(session, response.getItem(0));
    } catch (IOException | EncodeException e) {
      log.error(e.getMessage(), e);
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> getNames(Message message) {
    Object data = message.getData();
    return (List<String>) data;
  }
}
