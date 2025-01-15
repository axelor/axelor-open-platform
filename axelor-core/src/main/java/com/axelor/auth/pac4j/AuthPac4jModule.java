/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.pac4j.config.BaseConfig;
import com.axelor.auth.pac4j.ldap.AxelorLdapProfileService;
import com.axelor.auth.pac4j.local.AxelorAjaxRequestResolver;
import com.axelor.auth.pac4j.local.AxelorDirectBasicAuthClient;
import com.axelor.auth.pac4j.local.AxelorFormClient;
import com.axelor.auth.pac4j.local.AxelorIndirectBasicAuthClient;
import com.axelor.auth.pac4j.local.BasicAuthCallbackClientFinder;
import com.axelor.auth.pac4j.local.JsonExtractor;
import com.axelor.cache.CacheConfig;
import com.axelor.cache.CacheProviderInfo;
import com.axelor.cache.redisson.RedissonClientProvider;
import com.axelor.meta.MetaScanner;
import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletContext;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.cache.jcache.AxelorJCacheManager;
import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.session.mgt.eis.SessionDAO;
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
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthPac4jModule extends ShiroWebModule {

  public static final String CSRF_HEADER_NAME = "X-CSRF-Token";
  public static final String CSRF_COOKIE_NAME = "CSRF-TOKEN";

  private static final Logger log = LoggerFactory.getLogger(AuthPac4jModule.class);

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
    addFilterChain("/login", Key.get(AxelorLoginFilter.class));
    addFilterChain("/logout", Key.get(AxelorLogoutFilter.class));
    addFilterChain("/callback", Key.get(AxelorCallbackFilter.class));
    addFilterChain("/callback/**", Key.get(AxelorCallbackFilter.class));
    addFilterChain("/**", Key.get(AxelorSecurityFilter.class));

    bindRealm().to(AuthPac4jRealm.class);

    final Multibinder<AuthenticationListener> authListenerMultibinder =
        Multibinder.newSetBinder(binder(), AuthenticationListener.class);
    authListenerMultibinder.addBinding().to(AuthPac4jListener.class);

    final Class<? extends ClientListService> clientListService =
        MetaScanner.findSubTypesOf(ClientListService.class).find().stream()
            .filter(cls -> !ClientListDefaultService.class.equals(cls))
            .findFirst()
            .orElse(ClientListDefaultService.class);
    bindAndExpose(ClientListService.class).to(clientListService).asEagerSingleton();

    bindAndExpose(Clients.class).toProvider(ClientsProvider.class);
    bindAndExpose(Config.class).to(BaseConfig.class);

    bindAndExpose(FormClient.class).to(AxelorFormClient.class);
    bindAndExpose(FormExtractor.class).to(JsonExtractor.class);
    bindAndExpose(AjaxRequestResolver.class).to(AxelorAjaxRequestResolver.class);

    bindAndExpose(IndirectBasicAuthClient.class).to(AxelorIndirectBasicAuthClient.class);
    bindAndExpose(DirectBasicAuthClient.class).to(AxelorDirectBasicAuthClient.class);

    bindAndExpose(LdapProfileService.class).to(AxelorLdapProfileService.class);

    bindAndExpose(SessionDAO.class).to(EnterpriseCacheSessionDAO.class).in(Singleton.class);
    expose(new TypeLiteral<Configuration<Object, Object>>() {}).annotatedWith(Names.named("shiro"));
    expose(new TypeLiteral<CacheManager>() {}).annotatedWith(Names.named("shiro"));
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

  @Override
  protected void bindSessionManager(AnnotatedBindingBuilder<SessionManager> bind) {
    bind.to(AxelorSessionManager.class).asEagerSingleton();
  }

  @Provides
  @Named(AvailableAppSettings.SESSION_TIMEOUT)
  @Singleton
  public long sessionTimeoutMinutes() {
    return AppSettings.get().getInt(AvailableAppSettings.SESSION_TIMEOUT, 60);
  }

  @Provides
  @Named("shiro")
  @Singleton
  public CachingProvider cachingProvider() {
    final var providerName =
        CacheConfig.getShiroCacheProvider()
            .map(CacheProviderInfo::getCachingProvider)
            .map(Class::getName)
            .orElse(CacheConfig.DEFAULT_JCACHE_PROVIDER);

    log.info("JCache provider: {}", providerName);
    return Caching.getCachingProvider(providerName);
  }

  @Provides
  @Named("shiro")
  @Singleton
  public CacheManager cacheManager(@Named("shiro") CachingProvider cachingProvider) {
    final var cacheManager = cachingProvider.getCacheManager();
    Runtime.getRuntime().addShutdownHook(new Thread(cacheManager::close));
    return cacheManager;
  }

  /** Cache configuration with session timeout */
  @Provides
  @Named("shiro")
  @Singleton
  public Configuration<Object, Object> cacheConfig(
      @Named("shiro") CachingProvider cachingProvider,
      @Named(AvailableAppSettings.SESSION_TIMEOUT) long sessionTimeoutMinutes) {
    final Configuration<Object, Object> cacheConfig;

    // Generic fallback cache configuration
    final var jCacheConfig = new MutableConfiguration<Object, Object>();
    jCacheConfig.setTypes(Object.class, Object.class);
    jCacheConfig.setExpiryPolicyFactory(
        AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, sessionTimeoutMinutes)));

    // Specialized cache configuration
    if (cachingProvider instanceof CaffeineCachingProvider) {
      // Caffeine specific settings required
      final var config = new CaffeineConfiguration<Object, Object>(jCacheConfig);
      config.setExpireAfterAccess(OptionalLong.of(sessionTimeoutMinutes * 60_000_000_000L));
      cacheConfig = config;
    } else if (cachingProvider instanceof org.redisson.jcache.JCachingProvider) {
      final var provider =
          CacheConfig.getShiroCacheProvider()
              .orElseThrow(() -> new IllegalStateException("Shiro cache provider not configured"));
      final var redisson = RedissonClientProvider.getInstance().get(provider);
      cacheConfig = RedissonConfiguration.fromInstance(redisson, jCacheConfig);
    } else {
      cacheConfig = jCacheConfig;
    }

    return cacheConfig;
  }

  @Provides
  @Singleton
  public DefaultWebSecurityManager webSecurityManager(
      Collection<Realm> realms,
      Set<AuthenticationListener> authenticationListeners,
      ModularRealmAuthenticator authenticator,
      AxelorSessionManager sessionManager,
      AxelorRememberMeManager rememberMeManager,
      AxelorJCacheManager cacheManager) {

    final DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();

    securityManager.setCacheManager(cacheManager);
    securityManager.setRealms(realms);
    authenticator.setRealms(securityManager.getRealms());
    authenticator.setAuthenticationListeners(authenticationListeners);
    securityManager.setAuthenticator(authenticator);
    securityManager.setSessionManager(sessionManager);
    securityManager.setRememberMeManager(rememberMeManager);

    return securityManager;
  }

  @Provides
  @Singleton
  public DefaultCallbackClientFinder callbackClientFinder(ClientListService clientListService) {
    final Optional<String> clientName =
        clientListService.get().stream()
            .filter(IndirectBasicAuthClient.class::isInstance)
            .findFirst()
            .map(Client::getName);
    if (clientName.isPresent()) {
      return new BasicAuthCallbackClientFinder(clientName.get());
    }
    return new AxelorCallbackClientFinder();
  }
}
