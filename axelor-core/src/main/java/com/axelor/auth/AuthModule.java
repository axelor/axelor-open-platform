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
package com.axelor.auth;

import com.axelor.auth.pac4j.AuthPac4jModule;
import com.axelor.auth.pac4j.AuthPac4jObserver;
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

    // non-web environment (cli or unit tests)
    if (context == null) {
      install(new MyShiroModule());
      return;
    }

    // pac4j
    bind(AuthPac4jObserver.class);
    install(new AuthPac4jModule(context));
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
