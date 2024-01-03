/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j.local;

import com.axelor.common.StringUtils;
import com.axelor.event.Event;
import com.axelor.event.NamedLiteral;
import com.axelor.events.PostLogin;
import io.buji.pac4j.token.Pac4jToken;
import java.util.Collections;
import javax.inject.Inject;
import org.pac4j.core.client.Client;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.Pac4jConstants;

public class CredentialsHandler {

  @Inject private Event<PostLogin> postLogin;

  public void handleInvalidCredentials(Client client, String username, CredentialsException e) {
    final CommonProfile profile = new CommonProfile();
    profile.setClientName(client.getName());

    if (StringUtils.notBlank(username)) {
      profile.setId(username);
      profile.addAttribute(Pac4jConstants.USERNAME, username);
    }

    final Pac4jToken token = new Pac4jToken(Collections.singletonList(profile), false);

    postLogin.select(NamedLiteral.of(PostLogin.FAILURE)).fire(new PostLogin(token, null, e));
  }
}
