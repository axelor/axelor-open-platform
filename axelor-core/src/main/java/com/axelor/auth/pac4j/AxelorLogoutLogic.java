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

import com.axelor.auth.pac4j.local.AxelorFormClient;
import io.buji.pac4j.profile.ShiroProfileManager;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.DefaultLogoutLogic;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.http.ajax.AjaxRequestResolver;

@Singleton
public class AxelorLogoutLogic extends DefaultLogoutLogic {

  private final AjaxRequestResolver ajaxRequestResolver;
  private final boolean exclusiveLike;

  @Inject
  public AxelorLogoutLogic(
      AjaxRequestResolver ajaxRequestResolver,
      ClientListProvider clientListProvider,
      AxelorCallbackFilter callbackFilter,
      AxelorFormClient formClient) {
    this.ajaxRequestResolver = ajaxRequestResolver;
    this.exclusiveLike =
        clientListProvider.isExclusive()
            || !formClient.getName().equals(callbackFilter.getDefaultClient());
    setProfileManagerFactory(ShiroProfileManager::new);
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

    final String redirectUrl =
        exclusiveLike
                || Boolean.TRUE.equals(inputCentralLogout)
                || !ajaxRequestResolver.isAjax(context, sessionStore)
            ? defaultUrl
            : null;

    return super.perform(
        context,
        sessionStore,
        config,
        httpActionAdapter,
        redirectUrl,
        inputLogoutUrlPattern,
        inputLocalLogout,
        inputDestroySession,
        inputCentralLogout);
  }
}
