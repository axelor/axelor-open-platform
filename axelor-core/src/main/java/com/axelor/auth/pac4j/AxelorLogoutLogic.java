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
package com.axelor.auth.pac4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.DefaultLogoutLogic;
import org.pac4j.core.exception.http.NoContentAction;
import org.pac4j.core.exception.http.OkAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;

@Singleton
public class AxelorLogoutLogic extends DefaultLogoutLogic {

  private final AuthPac4jInfo info;

  @Inject
  public AxelorLogoutLogic(AuthPac4jInfo info) {
    this.info = info;
  }

  @Override
  public Object perform(
      WebContext context,
      SessionStore sessionStore,
      Config config,
      HttpActionAdapter httpActionAdapter,
      String defaultUrl,
      String inputLogoutUrlPattern,
      Boolean inputLocalLogout,
      Boolean inputDestroySession,
      Boolean inputCentralLogout) {

    final HttpActionAdapter ajaxAwareAdapter =
        (action, ctx) -> {
          if (action instanceof WithLocationAction && AuthPac4jInfo.isXHR(context)) {
            final WithLocationAction withLocationAction = (WithLocationAction) action;
            final String url = withLocationAction.getLocation();
            if (info.getBaseUrl().equals(url)) {
              action = NoContentAction.INSTANCE;
            } else {
              ctx.setResponseHeader(
                  HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.APPLICATION_JSON);
              final ObjectMapper mapper = new ObjectMapper();
              final ObjectNode json = mapper.createObjectNode();
              json.put("redirectUrl", url);
              action = new OkAction(json.toString());
            }
          }
          return httpActionAdapter.adapt(action, ctx);
        };

    return super.perform(
        context,
        sessionStore,
        config,
        ajaxAwareAdapter,
        defaultUrl,
        inputLogoutUrlPattern,
        inputLocalLogout,
        inputDestroySession,
        inputCentralLogout);
  }
}
