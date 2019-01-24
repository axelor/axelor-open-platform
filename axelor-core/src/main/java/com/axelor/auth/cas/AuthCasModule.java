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
package com.axelor.auth.cas;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthWebModule;
import com.axelor.common.StringUtils;
import com.google.inject.Key;
import com.google.inject.name.Names;
import javax.servlet.ServletContext;

public class AuthCasModule extends AuthWebModule {

  public AuthCasModule(ServletContext servletContext) {
    super(servletContext);
  }

  public static boolean isEnabled() {
    final AppSettings settings = AppSettings.get();
    final String casServerUrlPrefix = settings.get(AuthCasRealm.CONFIG_CAS_SERVER_PREFIX_URL, null);
    final String casService = settings.get(AuthCasRealm.CONFIG_CAS_SERVICE, null);
    return casServerUrlPrefix != null && casService != null;
  }

  @Override
  protected void configureAuth() {

    final AppSettings settings = AppSettings.get();
    final String casServerUrlPrefix = settings.get(AuthCasRealm.CONFIG_CAS_SERVER_PREFIX_URL);
    final String casService = settings.get(AuthCasRealm.CONFIG_CAS_SERVICE);

    String casLoginUrl = settings.get(AuthCasRealm.CONFIG_CAS_LOGIN_URL);
    String casLogoutUrl = settings.get(AuthCasRealm.CONFIG_CAS_LOGOUT_URL);
    String casProtocol = settings.get(AuthCasRealm.CONFIG_CAS_PROTOCOL);

    if (StringUtils.isBlank(casLoginUrl)) {
      casLoginUrl = String.format("%s/login?service=%s", casServerUrlPrefix, casService);
    }
    if (StringUtils.isBlank(casLogoutUrl)) {
      casLogoutUrl = String.format("%s/logout?service=%s", casServerUrlPrefix, casService);
    }
    if (StringUtils.isBlank(casProtocol)) {
      casProtocol = "SAML";
    }

    this.bindConstant().annotatedWith(Names.named("shiro.cas.failure.url")).to("/error.jsp");
    this.bindConstant()
        .annotatedWith(Names.named("shiro.cas.server.url.prefix"))
        .to(casServerUrlPrefix);
    this.bindConstant().annotatedWith(Names.named("shiro.cas.service")).to(casService);
    this.bindConstant().annotatedWith(Names.named("shiro.cas.login.url")).to(casLoginUrl);
    this.bindConstant().annotatedWith(Names.named("shiro.cas.logout.url")).to(casLogoutUrl);
    this.bindConstant().annotatedWith(Names.named("shiro.cas.protocol")).to(casProtocol);

    this.bindRealm().to(AuthCasRealm.class);
    this.addFilterChain("/cas", Key.get(AuthCasFilter.class));
    this.addFilterChain("/logout", Key.get(AuthCasLogoutFilter.class));
    this.addFilterChain("/**", Key.get(AuthCasUserFilter.class));
  }
}
