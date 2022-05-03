/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import javax.servlet.ServletContext;
import org.apache.shiro.guice.web.ShiroWebModule;

public abstract class AuthWebModule extends ShiroWebModule {

  protected AuthWebModule(ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected final void configureShiroWeb() {
    this.configureAnon();
    this.configureAuth();
  }

  protected void configureAnon() {
    this.addFilterChain("/ws/public/**", ANON);
    this.addFilterChain("/public/**", ANON);
    this.addFilterChain("/dist/**", ANON);
    this.addFilterChain("/lib/**", ANON);
    this.addFilterChain("/img/**", ANON);
    this.addFilterChain("/ico/**", ANON);
    this.addFilterChain("/css/**", ANON);
    this.addFilterChain("/js/**", ANON);
    this.addFilterChain("/error.jsp", ANON);
    this.addFilterChain("/favicon.ico", ANON);
  }

  protected abstract void configureAuth();
}
