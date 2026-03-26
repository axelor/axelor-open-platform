/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.auth.pac4j.AuthPac4jModule;
import com.axelor.auth.pac4j.AuthPac4jObserver;
import com.axelor.auth.password.PasswordPolicy;
import com.axelor.auth.password.policy.DigitsPasswordPolicy;
import com.axelor.auth.password.policy.LengthPasswordPolicy;
import com.axelor.auth.password.policy.LowerCasePasswordPolicy;
import com.axelor.auth.password.policy.NotCodePasswordPolicy;
import com.axelor.auth.password.policy.NotSamePasswordPolicy;
import com.axelor.auth.password.policy.PatternPasswordPolicy;
import com.axelor.auth.password.policy.ScorePasswordPolicy;
import com.axelor.auth.password.policy.SpecialCharsPasswordPolicy;
import com.axelor.auth.password.policy.UpperCasePasswordPolicy;
import com.axelor.db.JpaSecurity;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
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

    // bind password policies
    final var policies = Multibinder.newSetBinder(binder(), PasswordPolicy.class);
    policies.addBinding().to(NotSamePasswordPolicy.class);
    policies.addBinding().to(LengthPasswordPolicy.class);
    policies.addBinding().to(NotCodePasswordPolicy.class);
    policies.addBinding().to(DigitsPasswordPolicy.class);
    policies.addBinding().to(LowerCasePasswordPolicy.class);
    policies.addBinding().to(UpperCasePasswordPolicy.class);
    policies.addBinding().to(SpecialCharsPasswordPolicy.class);
    policies.addBinding().to(PatternPasswordPolicy.class);
    policies.addBinding().to(ScorePasswordPolicy.class);

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
