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
package com.axelor.auth.ldap;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthFilter;
import com.axelor.auth.AuthWebModule;
import com.google.inject.Key;
import javax.servlet.ServletContext;

public class AuthLdapModule extends AuthWebModule {

  public AuthLdapModule(ServletContext servletContext) {
    super(servletContext);
  }

  public static boolean isEnabled() {
    AppSettings settings = AppSettings.get();
    String ldapServerUrl = settings.get(AuthLdapService.LDAP_SERVER_URL, null);
    return ldapServerUrl != null;
  }

  @Override
  protected void configureAuth() {
    this.bindRealm().to(AuthLdapRealm.class);
    this.addFilterChain("/logout", LOGOUT);
    this.addFilterChain("/**", Key.get(AuthFilter.class));
  }
}
