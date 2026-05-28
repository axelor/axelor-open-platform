/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import jakarta.servlet.ServletContext;
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
    this.addFilterChain("/favicon.ico", ANON);
  }

  protected abstract void configureAuth();
}
