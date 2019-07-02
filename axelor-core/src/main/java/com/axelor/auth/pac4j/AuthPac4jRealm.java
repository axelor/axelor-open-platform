/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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

import com.axelor.auth.UserAuthenticationInfo;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.common.StringUtils;
import com.axelor.event.Event;
import com.axelor.events.PreLogin;
import io.buji.pac4j.realm.Pac4jRealm;
import io.buji.pac4j.token.Pac4jToken;
import java.util.List;
import javax.inject.Inject;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.pac4j.core.profile.CommonProfile;

public class AuthPac4jRealm extends Pac4jRealm {

  @Inject private UserRepository userRepo;
  @Inject private Event<PreLogin> preLogin;

  public AuthPac4jRealm() {
    setPrincipalNameAttribute("email");
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) {
    preLogin.fire(new PreLogin(authenticationToken));
    final Pac4jToken token = (Pac4jToken) authenticationToken;
    final List<CommonProfile> profiles = token.getProfiles();

    for (final CommonProfile profile : profiles) {
      User user = null;

      if (StringUtils.notBlank(profile.getUsername())) {
        user = userRepo.findByCode(profile.getUsername());
      }

      if (user == null && StringUtils.notBlank(profile.getEmail())) {
        user = userRepo.findByEmail(profile.getEmail());
      }

      if (user != null) {
        profile.addRole(AuthPac4jModule.ROLE_AUTH);
        profile.clearSensitiveData();
        profile.setRemembered(true);
        return new UserAuthenticationInfo(user.getCode(), profiles.hashCode(), getName(), user);
      }
    }

    return super.doGetAuthenticationInfo(token);
  }
}
