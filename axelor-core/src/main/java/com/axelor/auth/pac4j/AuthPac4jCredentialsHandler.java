/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j;

import com.axelor.common.StringUtils;
import com.axelor.event.Event;
import com.axelor.event.NamedLiteral;
import com.axelor.events.LoginRedirectException;
import com.axelor.events.PostLogin;
import com.axelor.inject.Beans;
import io.buji.pac4j.token.Pac4jToken;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.authc.CredentialsException;
import org.apache.shiro.web.util.WebUtils;
import org.pac4j.core.client.Client;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.profile.CommonProfile;

public class AuthPac4jCredentialsHandler {

  @Inject private Event<PostLogin> postLogin;

  public void handleInvalidCredentials(Client<?, ?> client, String username, String errorMessage) {
    final CommonProfile profile = new CommonProfile();
    profile.setClientName(client.getName());

    if (StringUtils.notBlank(username)) {
      profile.setId(username);
      profile.addAttribute(Pac4jConstants.USERNAME, username);
    }

    final Pac4jToken token = new Pac4jToken(Collections.singletonList(profile), false);

    try {
      postLogin
          .select(NamedLiteral.of(PostLogin.FAILURE))
          .fire(new PostLogin(token, null, new CredentialsException(errorMessage)));
    } catch (LoginRedirectException lre) {
      issueRedirect(lre.getLocation());
    }
  }

  private void issueRedirect(String url) {
    try {
      WebUtils.issueRedirect(
          Beans.get(HttpServletRequest.class), Beans.get(HttpServletResponse.class), url);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
