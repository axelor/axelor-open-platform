package com.axelor.auth;

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

	public AuthModule(ServletContext servletContext) {
		super(servletContext);
	}
	
	@Override @SuppressWarnings("unchecked")
	protected void configureShiroWeb() {
		

		this.bind(JpaSecurity.class).toProvider(AuthSecurity.class);
		this.expose(JpaSecurity.class);

		this.bindConstant().annotatedWith(Names.named("shiro.loginUrl")).to("/login.jsp");
		this.bindRealm().to(AuthRealm.class);

        this.addFilterChain("/public/**", ANON);
        this.addFilterChain("/lib/**", ANON);
		this.addFilterChain("/logout", LOGOUT);
		this.addFilterChain("/**", Key.get(AuthFilter.class));
	}
	
	public static class Simple extends ShiroModule {
		
		@Override
		protected void configureShiro() {
			this.bind(JpaSecurity.class).toProvider(AuthSecurity.class);
			this.expose(JpaSecurity.class);
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
