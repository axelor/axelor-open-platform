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

/**
 * Fires the post-login failure event for credentials rejected by the pac4j clients.
 *
 * <p>Clients validate credentials themselves, before any {@link Pac4jToken} is built and before
 * Shiro is involved. As a result, such failures never reach a realm and Shiro authentication
 * listeners are never notified, so the clients have to report the failure themselves.
 *
 * @see com.axelor.auth.pac4j.AuthPac4jListener#onFailure
 */
public class CredentialsHandler {

  @Inject private Event<PostLogin> postLogin;

  /**
   * Fires the post-login failure event for the given rejected credentials.
   *
   * <p>A token is built from the submitted username so that listeners observe the same event shape
   * as for logins that did reach Shiro, which is what failed login tracking relies on to identify
   * the account being attempted.
   *
   * @param client the client that rejected the credentials
   * @param username the submitted user identifier, may be blank if none was submitted
   * @param e the exception describing the failure
   */
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
