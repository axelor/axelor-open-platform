/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.UserAuthenticationInfo;
import com.axelor.auth.db.User;
import com.axelor.event.Event;
import com.axelor.events.PreLogin;
import io.buji.pac4j.realm.Pac4jRealm;
import io.buji.pac4j.token.Pac4jToken;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;

public class AuthPac4jRealm extends Pac4jRealm {

  @Inject private Event<PreLogin> preLogin;

  @Inject private AuthPac4jUserService userService;

  public AuthPac4jRealm() {
    final AppSettings settings = AppSettings.get();
    setPrincipalNameAttribute(
        settings.get(AvailableAppSettings.AUTH_USER_PRINCIPAL_ATTRIBUTE, "email"));
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) {
    preLogin.fire(new PreLogin(authenticationToken));

    final Pac4jToken token = (Pac4jToken) authenticationToken;
    final List<UserProfile> profiles = token.getProfiles();

    @SuppressWarnings("unchecked")
    final Optional<CommonProfile> profileOpt = (Optional<CommonProfile>) token.getPrincipal();

    if (profileOpt.isPresent()) {
      final CommonProfile profile = profileOpt.get();
      final User user = userService.getUser(profile);

      if (user != null && AuthUtils.isActive(user)) {
        return new UserAuthenticationInfo(user.getCode(), profiles.hashCode(), getName(), user);
      }
    }

    return super.doGetAuthenticationInfo(token);
  }
}
