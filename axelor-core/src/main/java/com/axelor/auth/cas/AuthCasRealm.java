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
package com.axelor.auth.cas;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.google.inject.persist.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.cas.CasRealm;

public class AuthCasRealm extends CasRealm {

  public static final String CONFIG_CAS_SERVER_PREFIX_URL = "cas.server.url.prefix";
  public static final String CONFIG_CAS_SERVICE = "cas.service";
  public static final String CONFIG_CAS_LOGIN_URL = "cas.login.url";
  public static final String CONFIG_CAS_LOGOUT_URL = "cas.logout.url";
  public static final String CONFIG_CAS_PROTOCOL = "cas.protocol";

  public static final String CONFIG_CAS_ATTRS_USER_NAME = "cas.attrs.user.name";
  public static final String CONFIG_CAS_ATTRS_USER_EMAIL = "cas.attrs.user.email";

  @Inject
  @Override
  public void setCasServerUrlPrefix(
      @Named("shiro.cas.server.url.prefix") String casServerUrlPrefix) {
    super.setCasServerUrlPrefix(casServerUrlPrefix);
  }

  @Inject
  @Override
  public void setCasService(@Named("shiro.cas.service") String casService) {
    super.setCasService(casService);
  }

  @Inject
  @Override
  public void setValidationProtocol(@Named("shiro.cas.protocol") String validationProtocol) {
    super.setValidationProtocol(validationProtocol);
  }

  @Override
  @Transactional
  @SuppressWarnings("all")
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
      throws AuthenticationException {

    final SimpleAuthenticationInfo info =
        (SimpleAuthenticationInfo) super.doGetAuthenticationInfo(token);
    final List<?> principals = info.getPrincipals().asList();

    if (principals.isEmpty()) {
      return null;
    }

    final Map<String, String> attrs = new HashMap<>();
    try {
      attrs.putAll((Map) principals.get(1));
    } catch (Exception e) {
    }

    AppSettings settings = AppSettings.get();
    AuthService service = AuthService.getInstance();
    Inflector inflector = Inflector.getInstance();

    String code = (String) principals.get(0);
    User user = AuthUtils.getUser(code);

    // generate user object
    if (user == null) {

      String name = attrs.get(settings.get(CONFIG_CAS_ATTRS_USER_NAME, "name"));
      String email = attrs.get(settings.get(CONFIG_CAS_ATTRS_USER_EMAIL, "mail"));

      if (StringUtils.isBlank(name)) {
        name = inflector.titleize(code.replace(".", " "));
      }

      user = new User(code, name);
      user.setEmail(email);
      user.setPassword(UUID.randomUUID().toString());
      user = JPA.save(user);
      service.encrypt(user);
    }

    if (!AuthUtils.isActive(user)) {
      return null;
    }

    return info;
  }
}
