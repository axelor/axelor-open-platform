/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j;

import com.axelor.auth.pac4j.ldap.AxelorLdapProfileService;
import com.axelor.auth.pac4j.local.AxelorAjaxRequestResolver;
import com.axelor.auth.pac4j.local.AxelorDirectBasicAuthClient;
import com.axelor.auth.pac4j.local.AxelorFormClient;
import com.axelor.auth.pac4j.local.AxelorIndirectBasicAuthClient;
import com.axelor.auth.pac4j.local.BasicAuthCallbackClientFinder;
import com.axelor.auth.pac4j.local.JsonExtractor;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.servlet.ServletContext;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.finder.DefaultCallbackClientFinder;
import org.pac4j.core.config.Config;
import org.pac4j.core.credentials.extractor.FormExtractor;
import org.pac4j.core.http.ajax.AjaxRequestResolver;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
import org.pac4j.ldap.profile.service.LdapProfileService;

public class AuthPac4jModule extends ShiroWebModule {

  public static final String CSRF_HEADER_NAME = "X-CSRF-Token";
  public static final String CSRF_COOKIE_NAME = "CSRF-TOKEN";

  public AuthPac4jModule(ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected void configureShiroWeb() {
    addFilterChain("/ws/public/**", ANON);
    addFilterChain("/public/**", ANON);
    addFilterChain("/dist/**", ANON);
    addFilterChain("/lib/**", ANON);
    addFilterChain("/img/**", ANON);
    addFilterChain("/ico/**", ANON);
    addFilterChain("/css/**", ANON);
    addFilterChain("/js/**", ANON);
    addFilterChain("/assets/**", ANON);
    addFilterChain("/index.html", ANON);
    addFilterChain("/manifest.json", ANON);
    addFilterChain("/favicon.ico", ANON);
    addFilterChain("/logout", Key.get(AxelorLogoutFilter.class));
    addFilterChain("/callback", Key.get(AxelorCallbackFilter.class));
    addFilterChain("/callback/**", Key.get(AxelorCallbackFilter.class));
    addFilterChain("/**", Key.get(AxelorSecurityFilter.class));

    bindRealm().to(AuthPac4jRealm.class);

    final Multibinder<AuthenticationListener> listenerMultibinder =
        Multibinder.newSetBinder(binder(), AuthenticationListener.class);
    listenerMultibinder.addBinding().to(AuthPac4jListener.class);

    bindAndExpose(new TypeLiteral<List<Client>>() {})
        .toProvider(ClientListProvider.class)
        .asEagerSingleton();
    bindAndExpose(Clients.class).toProvider(ClientsProvider.class);
    bindAndExpose(Config.class).toProvider(ConfigProvider.class);

    bindAndExpose(FormClient.class).to(AxelorFormClient.class);
    bindAndExpose(FormExtractor.class).to(JsonExtractor.class);
    bindAndExpose(AjaxRequestResolver.class).to(AxelorAjaxRequestResolver.class);

    bindAndExpose(IndirectBasicAuthClient.class).to(AxelorIndirectBasicAuthClient.class);
    bindAndExpose(DirectBasicAuthClient.class).to(AxelorDirectBasicAuthClient.class);

    bindAndExpose(LdapProfileService.class).to(AxelorLdapProfileService.class);
  }

  protected <T> AnnotatedBindingBuilder<T> bindAndExpose(Class<T> cls) {
    expose(cls);
    return bind(cls);
  }

  protected <T> AnnotatedBindingBuilder<T> bindAndExpose(TypeLiteral<T> typeLiteral) {
    expose(typeLiteral);
    return bind(typeLiteral);
  }

  @Override
  protected void bindWebSecurityManager(AnnotatedBindingBuilder<? super WebSecurityManager> bind) {
    bind.to(DefaultWebSecurityManager.class);
  }

  @Provides
  public DefaultWebSecurityManager webSecurityManager(
      Collection<Realm> realms,
      Set<AuthenticationListener> authenticationListeners,
      ModularRealmAuthenticator authenticator,
      AxelorSessionManager sessionManager,
      AxelorRememberMeManager rememberMeManager) {
    final DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();

    securityManager.setRealms(realms);
    authenticator.setRealms(securityManager.getRealms());
    authenticator.setAuthenticationListeners(authenticationListeners);
    securityManager.setAuthenticator(authenticator);
    securityManager.setSessionManager(sessionManager);
    securityManager.setRememberMeManager(rememberMeManager);

    return securityManager;
  }

  @Provides
  public DefaultCallbackClientFinder callbackClientFinder(List<Client> clients) {
    final Optional<String> clientName =
        clients.stream()
            .filter(IndirectBasicAuthClient.class::isInstance)
            .findFirst()
            .map(Client::getName);
    if (clientName.isPresent()) {
      return new BasicAuthCallbackClientFinder(clientName.get());
    }
    return new AxelorCallbackClientFinder();
  }
}
