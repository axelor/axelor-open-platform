/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import com.axelor.common.StringUtils;
import com.axelor.event.Event;
import com.axelor.event.NamedLiteral;
import com.axelor.events.PostLogin;
import io.buji.pac4j.token.Pac4jToken;
import jakarta.inject.Inject;
import java.util.Collections;
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
