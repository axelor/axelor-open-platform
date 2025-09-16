/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.AuthSecurityWarner;
import com.axelor.db.JpaSecurity;
import com.axelor.event.Event;
import com.axelor.events.PostAction;
import com.axelor.events.PreAction;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ActionExecutor {

  private final Event<PreAction> preActionEvent;
  private final Event<PostAction> postActionEvent;
  private final JpaSecurity security;

  @Inject
  ActionExecutor(
      Event<PreAction> preActionEvent, Event<PostAction> postActionEvent, JpaSecurity security) {
    this.preActionEvent = preActionEvent;
    this.postActionEvent = postActionEvent;
    this.security =
        AppSettings.get()
                .getBoolean(AvailableAppSettings.APPLICATION_PERMISSION_DISABLE_ACTION, false)
            ? Beans.get(AuthSecurityWarner.class)
            : security;
  }

  public ActionHandler newActionHandler(ActionRequest request) {
    return new ActionHandler(request, preActionEvent, postActionEvent, security);
  }

  public ActionResponse execute(ActionRequest request) {
    return newActionHandler(request).execute();
  }

  Event<PreAction> getPreActionEvent() {
    return preActionEvent;
  }

  Event<PostAction> getPostActionEvent() {
    return postActionEvent;
  }

  JpaSecurity getSecurity() {
    return security;
  }
}
