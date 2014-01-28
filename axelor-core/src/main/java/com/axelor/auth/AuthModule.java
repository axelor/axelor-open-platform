/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.auth;

import java.util.Properties;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.mgt.SecurityManager;

import com.axelor.db.JpaSecurity;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public class AuthModule extends ShiroWebModule {

	private Properties properties = new Properties();

	public AuthModule properties(Properties properties) {
		this.properties = properties;
		return this;
	}

	public AuthModule(ServletContext servletContext) {
		super(servletContext);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void configureShiroWeb() {

		this.bind(JpaSecurity.class).toProvider(AuthSecurity.class);
		this.expose(JpaSecurity.class);

		this.bindConstant().annotatedWith(Names.named("auth.hash.algorithm")).to("SHA-512");
		this.bindConstant().annotatedWith(Names.named("auth.hash.iterations")).to(500000);
		this.bind(Properties.class).annotatedWith(Names.named("auth.ldap.config")).toInstance(properties);

		this.bind(AuthService.class).asEagerSingleton();
		this.bind(AuthLdap.class).asEagerSingleton();

		this.expose(AuthService.class);
		this.expose(AuthLdap.class);

		this.bindConstant().annotatedWith(Names.named("app.loginUrl")).to("/login.jsp");
		this.bindRealm().to(AuthRealm.class);

		this.addFilterChain("/public/**", ANON);
		this.addFilterChain("/lib/**", ANON);
		this.addFilterChain("/img/**", ANON);
		this.addFilterChain("/ico/**", ANON);
		this.addFilterChain("/css/**", ANON);
		this.addFilterChain("/logout", LOGOUT);
		this.addFilterChain("/**", Key.get(AuthFilter.class));
	}

	public static class Simple extends ShiroModule {

		private Properties properties = new Properties();

		public Simple properties(Properties properties) {
			this.properties = properties;
			return this;
		}

		@Override
		protected void configureShiro() {

			this.bind(JpaSecurity.class).toProvider(AuthSecurity.class);
			this.expose(JpaSecurity.class);

			this.bindConstant().annotatedWith(Names.named("auth.hash.algorithm")).to("SHA-512");
			this.bindConstant().annotatedWith(Names.named("auth.hash.iterations")).to(500000);
			this.bind(Properties.class).annotatedWith(Names.named("auth.ldap.config")).toInstance(properties);

			this.bind(AuthService.class).asEagerSingleton();
			this.bind(AuthLdap.class).asEagerSingleton();

			this.expose(AuthService.class);
			this.expose(AuthLdap.class);

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
