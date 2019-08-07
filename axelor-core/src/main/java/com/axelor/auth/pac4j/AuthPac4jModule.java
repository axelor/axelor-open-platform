/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthWebModule;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import io.buji.pac4j.engine.ShiroCallbackLogic;
import io.buji.pac4j.filter.CallbackFilter;
import io.buji.pac4j.filter.LogoutFilter;
import io.buji.pac4j.filter.SecurityFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.util.WebUtils;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.http.client.indirect.FormClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AuthPac4jModule extends AuthWebModule {

  public static final String CONFIG_AUTH_CALLBACK_URL = "auth.callback.url";
  public static final String CONFIG_AUTH_DEFAULT_URL = "auth.default.url";
  public static final String CONFIG_AUTH_SAVE_USERS_FROM_CENTRAL = "auth.save.users.from.central";
  public static final String CONFIG_AUTH_PRINCIPAL_ATTRIBUTE = "auth.principal.attribute";

  public static final String CONFIG_AUTH_LOGOUT_DEFAULT_URL = "auth.logout.default.url";
  public static final String CONFIG_AUTH_LOGOUT_URL_PATTERN = "auth.logout.url.pattern";
  public static final String CONFIG_AUTH_LOGOUT_LOCAL = "auth.logout.local";
  public static final String CONFIG_AUTH_LOGOUT_CENTRAL = "auth.logout.central";

  protected static final String ROLE_HAS_USER = "_ROLE_HAS_USER";

  @SuppressWarnings("rawtypes")
  private List<Client> clientList = new ArrayList<>();

  private static final Set<String> centralClientNames = new LinkedHashSet<>();

  private static final Map<String, Map<String, String>> clientInfo = new HashMap<>();

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public AuthPac4jModule(ServletContext servletContext) {
    super(servletContext);
    logger.info("Loading pac4j: {}", getClass().getSimpleName());
  }

  @Nullable
  public static Map<String, String> getClientInfo(String clientName) {
    return clientInfo.get(clientName);
  }

  protected static void setClientInfo(String clientName, Map<String, String> info) {
    clientInfo.put(clientName, info);
  }

  @Override
  protected void configureAuth() {
    configureClients();

    final Multibinder<AuthenticationListener> listenerMultibinder =
        Multibinder.newSetBinder(binder(), AuthenticationListener.class);
    listenerMultibinder.addBinding().to(AuthPac4jListener.class);

    final AppSettings settings = AppSettings.get();
    String callbackUrl = settings.get(CONFIG_AUTH_CALLBACK_URL, null);

    // Backward-compatible CAS configuration
    if (callbackUrl == null && AuthPac4jModuleCas.isEnabled()) {
      callbackUrl = settings.get(AuthPac4jModuleCas.CONFIG_CAS_SERVICE, null);
    }

    final Clients clients = new Clients(callbackUrl, clientList);
    final Authorizer<CommonProfile> authorizer = new RequireAnyRoleAuthorizer<>(ROLE_HAS_USER);
    final Config config = new Config(clients, ImmutableMap.of("auth", authorizer));

    bind(Config.class).toInstance(config);
    bindRealm().to(AuthPac4jRealm.class);
    addFilterChain("/logout", Key.get(Pac4jLogoutFilter.class));
    addFilterChain("/callback", Key.get(Pac4jCallbackFilter.class));
    addFilterChain("/**", Key.get(Pac4jSecurityFilter.class));
  }

  protected abstract void configureClients();

  protected void addClient(FormClient client) {
    clientList.add(0, client);
    logger.info("Added local client: {}", client.getName());
  }

  protected void addClient(Client<?, ?> client) {
    clientList.add(client);
    centralClientNames.add(client.getName());
    logger.info("Added central client: {}", client.getName());
  }

  public static Set<String> getCentralClients() {
    return centralClientNames;
  }

  public static boolean isEnabled() {
    final AppSettings settings = AppSettings.get();
    return settings.get(CONFIG_AUTH_CALLBACK_URL, null) != null;
  }

  @Override
  protected void bindWebSecurityManager(AnnotatedBindingBuilder<? super WebSecurityManager> bind) {
    bind.to(DefaultWebSecurityManager.class);
  }

  @Provides
  protected DefaultWebSecurityManager provideDefaultSecurityManager(
      Collection<Realm> realms, Set<AuthenticationListener> authenticationListeners) {
    DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager(realms);
    ModularRealmAuthenticator authenticator = new ModularRealmAuthenticator();
    authenticator.setRealms(realms);
    authenticator.setAuthenticationListeners(authenticationListeners);
    securityManager.setAuthenticator(authenticator);
    return securityManager;
  }

  protected static boolean isXHR(WebContext context) {
    return context instanceof J2EContext && isXHR(((J2EContext) context).getRequest());
  }

  protected static boolean isXHR(HttpServletRequest request) {
    return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
        || "application/json".equals(request.getHeader("Accept"))
        || "application/json".equals(request.getHeader("Content-Type"));
  }

  private static class Pac4jLogoutFilter extends LogoutFilter {

    @Inject
    public Pac4jLogoutFilter(Config config) {
      setConfig(config);

      final AppSettings settings = AppSettings.get();

      // Backward-compatible CAS configuration
      String defaultUrl = settings.get(CONFIG_AUTH_LOGOUT_DEFAULT_URL, null);
      if (defaultUrl == null) {
        defaultUrl =
            AuthPac4jModuleCas.isEnabled()
                ? settings.get(AuthPac4jModuleCas.CONFIG_CAS_LOGOUT_URL, ".")
                : ".";
      }

      final String logoutUrlPattern = settings.get(CONFIG_AUTH_LOGOUT_URL_PATTERN, null);
      final boolean localLogout = settings.getBoolean(CONFIG_AUTH_LOGOUT_LOCAL, true);
      final boolean centralLogout = settings.getBoolean(CONFIG_AUTH_LOGOUT_CENTRAL, false);

      setDefaultUrl(defaultUrl);
      setLogoutUrlPattern(logoutUrlPattern);
      setLocalLogout(localLogout);
      setCentralLogout(centralLogout);
    }
  }

  private static class Pac4jCallbackFilter extends CallbackFilter {

    @Inject
    public Pac4jCallbackFilter(Config config) {
      setConfig(config);

      final AppSettings settings = AppSettings.get();
      final String defaultUrl = settings.get(CONFIG_AUTH_DEFAULT_URL, settings.getBaseURL());

      if (defaultUrl != null) {
        setDefaultUrl(defaultUrl);
      }

      setDefaultClient(config.getClients().getClients().get(0).getName());
      setCallbackLogic(
          new ShiroCallbackLogic<Object, J2EContext>() {

            @Override
            protected HttpAction redirectToOriginallyRequestedUrl(
                J2EContext context, String defaultUrl) {
              return isXHR(context)
                  ? HttpAction.status(HttpConstants.OK, context)
                  : super.redirectToOriginallyRequestedUrl(context, defaultUrl);
            }
          });
    }

    @Override
    public void doFilter(
        ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
      super.doFilter(servletRequest, servletResponse, filterChain);

      // remove client_name query param after central login
      final HttpServletRequest req = (HttpServletRequest) servletRequest;

      if (SecurityUtils.getSubject().isAuthenticated() && req.getParameter("client_name") != null) {
        WebUtils.issueRedirect(servletRequest, servletResponse, "/");
      }
    }
  }

  private static class Pac4jSecurityFilter extends SecurityFilter {

    @Inject
    public Pac4jSecurityFilter(Config config) {
      setConfig(config);
      setAuthorizers("auth");

      final String clientNames =
          config
              .getClients()
              .getClients()
              .stream()
              .map(Client::getName)
              .collect(Collectors.joining(","));
      setClients(clientNames);
    }
  }
}
