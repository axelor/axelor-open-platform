/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.web.socket.channels;

import com.axelor.mail.web.MailController;
import com.axelor.meta.service.tags.TagsService;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.axelor.team.web.TaskController;
import com.axelor.web.socket.Channel;
import com.axelor.web.socket.Message;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;
import javax.websocket.EncodeException;
import javax.websocket.Session;
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
  public void onSubscribe(Session session) {}

  @Override
  public void onUnsubscribe(Session session) {}

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
