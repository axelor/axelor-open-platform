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
package com.axelor.auth;

import java.util.Properties;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.mgt.SecurityManager;

import com.axelor.db.JpaSecurity;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public class AuthModule extends AbstractModule {

	private Properties properties = new Properties();

	private ServletContext context;

	public AuthModule() {
	}

	public AuthModule(ServletContext context) {
		this.context = context;
	}

	public AuthModule properties(Properties properties) {
		this.properties = properties;
		return this;
	}

	@Override
	protected final void configure() {

		this.bindConstant().annotatedWith(Names.named("app.loginUrl")).to("/login.jsp");
		this.bindConstant().annotatedWith(Names.named("auth.hash.algorithm")).to("SHA-512");
		this.bindConstant().annotatedWith(Names.named("auth.hash.iterations")).to(500000);
		this.bind(Properties.class).annotatedWith(Names.named("auth.ldap.config")).toInstance(properties);

		this.bind(JpaSecurity.class).toProvider(AuthSecurity.class);
		this.bind(AuthService.class).asEagerSingleton();

		this.configureAuth();

		// initialize SecurityManager
		this.bind(Initializer.class).asEagerSingleton();
	}

	protected void configureAuth() {
		final Module module = context == null ? new MyShiroModule() : new MyShiroWebModule(context);
		this.install(module);
	}

	static final class MyShiroModule extends ShiroModule {

		@Override
		protected void configureShiro() {
			this.bindRealm().to(AuthRealm.class);
		}
	}

	static final class MyShiroWebModule extends ShiroWebModule {

		public MyShiroWebModule(ServletContext context) {
			super(context);
		}

		@Override
		@SuppressWarnings("unchecked")
		protected void configureShiroWeb() {
			this.addFilterChain("/public/**", ANON);
			this.addFilterChain("/dist/**", ANON);
			this.addFilterChain("/lib/**", ANON);
			this.addFilterChain("/img/**", ANON);
			this.addFilterChain("/ico/**", ANON);
			this.addFilterChain("/css/**", ANON);
			this.addFilterChain("/js/**", ANON);
			this.addFilterChain("/error.jsp", ANON);

			this.bindRealm().to(AuthRealm.class);
			this.addFilterChain("/logout", LOGOUT);
			this.addFilterChain("/**", Key.get(AuthFilter.class));
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
