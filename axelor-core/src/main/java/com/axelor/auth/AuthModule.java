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
package com.axelor.auth;

import com.axelor.app.AppSettings;
import com.axelor.auth.ldap.AuthLdapModule;
import com.axelor.auth.pac4j.AuthPac4jModule;
import com.axelor.auth.pac4j.AuthPac4jModuleCas;
import com.axelor.auth.pac4j.AuthPac4jModuleLocal;
import com.axelor.auth.pac4j.AuthPac4jModuleOAuth;
import com.axelor.auth.pac4j.AuthPac4jModuleOidc;
import com.axelor.auth.pac4j.AuthPac4jModuleSaml;
import com.axelor.auth.pac4j.AuthPac4jObserverCreate;
import com.axelor.auth.pac4j.AuthPac4jObserverLink;
import com.axelor.db.JpaSecurity;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.mgt.SecurityManager;

public class AuthModule extends AbstractModule {

  private ServletContext context;

  public AuthModule() {}

  public AuthModule(ServletContext context) {
    this.context = context;
  }

  @Override
  protected final void configure() {

    // bind security service
    bind(JpaSecurity.class).toProvider(AuthSecurity.class);

    // observe authentication-related events
    bind(AuthObserver.class);

    // non-web environment (cli or unit tests)
    if (context == null) {
      install(new MyShiroModule());
      return;
    }

    // LDAP
    if (AuthLdapModule.isEnabled()) {
      install(new AuthLdapModule(context));
      return;
    }

    // Pac4j
    if (AuthPac4jModule.isEnabled()) {
      final AppSettings settings = AppSettings.get();
      final String userProvisioning =
          settings.get(AuthPac4jModule.CONFIG_AUTH_USER_PROVISIONING, "create");

      // User provisioning
      switch (userProvisioning) {
        case "create":
          // Create and update users
          bind(AuthPac4jObserverCreate.class);
          break;
        case "link":
          // Update users (must exist locally beforehand)
          bind(AuthPac4jObserverLink.class);
          break;
        default:
      }

      // OpenID Connect
      if (AuthPac4jModuleOidc.isEnabled()) {
        install(new AuthPac4jModuleOidc(context));
        return;
      }

      // OAuth
      if (AuthPac4jModuleOAuth.isEnabled()) {
        install(new AuthPac4jModuleOAuth(context));
        return;
      }

      // SAML
      if (AuthPac4jModuleSaml.isEnabled()) {
        install(new AuthPac4jModuleSaml(context));
        return;
      }

      // CAS
      if (AuthPac4jModuleCas.isEnabled()) {
        install(new AuthPac4jModuleCas(context));
        return;
      }

      // Local
      install(new AuthPac4jModuleLocal(context));
      return;
    }

    // Default
    install(new AuthWebModule(context));
  }

  static final class MyShiroModule extends ShiroModule {

    @Override
    protected void configureShiro() {
      this.bindRealm().to(AuthRealm.class);
      this.bind(Initializer.class).asEagerSingleton();
    }
  }

  @Singleton
  public static class Initializer {

    @Inject
    public Initializer(Injector injector) {
      SecurityManager sm = injector.getInstance(SecurityManager.class);
      SecurityUtils.setSecurityManager(sm);
    }
  }
}
