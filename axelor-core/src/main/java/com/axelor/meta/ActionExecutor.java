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
package com.axelor.meta;

import com.axelor.event.Event;
import com.axelor.events.PostAction;
import com.axelor.events.PreAction;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import javax.inject.Inject;

public class ActionExecutor {

  private final Event<PreAction> preActionEvent;
  private final Event<PostAction> postActionEvent;

  @Inject
  ActionExecutor(Event<PreAction> preActionEvent, Event<PostAction> postActionEvent) {
    this.preActionEvent = preActionEvent;
    this.postActionEvent = postActionEvent;
  }

  public ActionHandler newActionHandler(ActionRequest request) {
    return new ActionHandler(request, preActionEvent, postActionEvent);
  }

  public ActionResponse execute(ActionRequest request) {
    return newActionHandler(request).execute();
  }
}
