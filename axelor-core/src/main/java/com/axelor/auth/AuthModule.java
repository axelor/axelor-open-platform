/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
