/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2020 Axelor (<http://axelor.com>).
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
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.AuthWebModule;
import com.axelor.common.StringUtils;
import com.google.common.base.Preconditions;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import io.buji.pac4j.context.ShiroSessionStore;
import io.buji.pac4j.engine.ShiroCallbackLogic;
import io.buji.pac4j.engine.ShiroSecurityLogic;
import io.buji.pac4j.filter.CallbackFilter;
import io.buji.pac4j.filter.LogoutFilter;
import io.buji.pac4j.filter.SecurityFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.authorization.authorizer.csrf.CsrfAuthorizer;
import org.pac4j.core.authorization.authorizer.csrf.CsrfTokenGenerator;
import org.pac4j.core.authorization.authorizer.csrf.DefaultCsrfTokenGenerator;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.http.adapter.J2ENopHttpActionAdapter;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.http.client.indirect.FormClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AuthPac4jModule extends AuthWebModule {

  @SuppressWarnings("rawtypes")
  private static final List<Client> clientList = new ArrayList<>();

  private static final Map<String, Map<String, String>> clientInfo = new HashMap<>();
  private static final Set<String> centralClientNames = new LinkedHashSet<>();

  private static String callbackUrl;
  private static boolean absCallbackUrlRequired;
  private static String logoutUrl;

  private static final String CSRF_TOKEN_AUTHORIZER_NAME = "axelorCsrfToken";
  private static final String CSRF_AUTHORIZER_NAME = "axelorCsrf";

  private static final String CSRF_COOKIE_NAME = "CSRF-TOKEN";
  private static final String CSRF_HEADER_NAME = "X-CSRF-Token";

  private static final String HASH_LOCATION_PARAMETER = "hash_location";

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

    bind(Config.class).toProvider(ConfigProvider.class);
    bindRealm().to(AuthPac4jRealm.class);
    addFilterChain("/login.jsp", Key.get(AxelorLoginPageFilter.class));
    addFilterChain("/logout", Key.get(AxelorLogoutFilter.class));
    addFilterChain("/callback", Key.get(AxelorCallbackFilter.class));
    addFilterChain("/**", Key.get(AxelorSecurityFilter.class));
  }

  protected abstract void configureClients();

  protected void addLocalClient(Client<?, ?> client) {
    Preconditions.checkState(
        centralClientNames.isEmpty(), "Local clients must be added before central clients.");
    addClient(client);
    logger.info("Added local client: {}", client.getName());
  }

  protected void addCentralClient(Client<?, ?> client) {
    addClient(client);
    centralClientNames.add(client.getName());
    logger.info("Added central client: {}", client.getName());
  }

  private void addClient(Client<?, ?> client) {
    clientList.add(client);
  }

  public static Set<String> getCentralClients() {
    return centralClientNames;
  }

  public static String getRelativeBaseURL() {
    String base = AppSettings.get().getBaseURL();
    return URI.create(base).getPath();
  }

  public static String getCallbackUrl() {
    if (callbackUrl == null) {
      final AppSettings settings = AppSettings.get();
      callbackUrl = settings.get(AvailableAppSettings.AUTH_CALLBACK_URL, null);

      // Backward-compatible CAS configuration
      if (StringUtils.isBlank(callbackUrl) && AuthPac4jModuleCas.isEnabled()) {
        callbackUrl = settings.get(AvailableAppSettings.AUTH_CAS_SERVICE, null);
      }

      if (StringUtils.isBlank(callbackUrl)) {
        String base = AppSettings.get().getBaseURL();
        if (!absCallbackUrlRequired) {
          base = URI.create(base).getPath();
        }
        callbackUrl = base + "/callback";
      }
    }

    return callbackUrl;
  }

  public static void requireAbsCallbackUrl() {
    absCallbackUrlRequired = true;
  }

  public static String getLogoutUrl() {
    if (logoutUrl == null) {
      // Backward-compatible CAS configuration
      final AppSettings settings = AppSettings.get();
      logoutUrl = settings.get(AvailableAppSettings.AUTH_LOGOUT_URL, null);
      if (StringUtils.isBlank(logoutUrl)) {
        logoutUrl =
            AuthPac4jModuleCas.isEnabled()
                ? settings.get(AvailableAppSettings.AUTH_CAS_LOGOUT_URL, getRelativeBaseURL())
                : getRelativeBaseURL();
      }
      if (StringUtils.isBlank(logoutUrl)) {
        logoutUrl = ".";
      }
    }

    return logoutUrl;
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

  protected static boolean isNativeClient(WebContext context) {
    String origin = context.getRequestHeader("Origin");
    return StringUtils.isBlank(origin);
  }

  @Singleton
  private static class ConfigProvider implements Provider<Config> {

    private Config config;

    @Inject
    public ConfigProvider() {
      final Clients clients = new Clients(getCallbackUrl(), clientList);
      final Map<String, Authorizer<?>> authorizers = new LinkedHashMap<>();

      authorizers.put(CSRF_TOKEN_AUTHORIZER_NAME, new AxelorCsrfTokenGeneratorAuthorizer());
      authorizers.put(CSRF_AUTHORIZER_NAME, new AxelorCsrfAuthorizer());

      this.config = new Config(clients, Collections.unmodifiableMap(authorizers));
    }

    @Override
    public Config get() {
      return config;
    }
  }

  private static class AxelorCsrfTokenGeneratorAuthorizer implements Authorizer<CommonProfile> {

    private final CsrfTokenGenerator tokenGenerator = new DefaultCsrfTokenGenerator();

    @Override
    public boolean isAuthorized(WebContext context, List<CommonProfile> profiles) {

      // don't need csrf check for native clients
      if (isNativeClient(context)) return true;

      addResponseCookieAndHeader(context);
      return true;
    }

    private void addResponseCookieAndHeader(WebContext context) {
      final String token = tokenGenerator.get(context);
      final Cookie cookie = new Cookie(CSRF_COOKIE_NAME, token);

      String path = ((J2EContext) context).getRequest().getContextPath();
      if (path.length() == 0) {
        path = "/";
      }

      cookie.setDomain("");
      cookie.setPath(path);
      context.addResponseCookie(cookie);
      context.setResponseHeader(CSRF_HEADER_NAME, token);
    }
  }

  private static class AxelorCsrfAuthorizer extends CsrfAuthorizer {

    public AxelorCsrfAuthorizer() {
      super(CSRF_HEADER_NAME, CSRF_HEADER_NAME);
    }

    @Override
    public boolean isAuthorized(WebContext context, List<CommonProfile> profiles) {
      return isNativeClient(context) || super.isAuthorized(context, profiles);
    }
  }

  private static class AxelorLogoutFilter extends LogoutFilter {

    @Inject
    public AxelorLogoutFilter(Config config) {
      final AppSettings settings = AppSettings.get();
      final String logoutUrlPattern =
          settings.get(AvailableAppSettings.AUTH_LOGOUT_URL_PATTERN, null);
      final boolean localLogout = settings.getBoolean(AvailableAppSettings.AUTH_LOGOUT_LOCAL, true);
      final boolean centralLogout =
          settings.getBoolean(AvailableAppSettings.AUTH_LOGOUT_CENTRAL, false);

      setConfig(config);
      setDefaultUrl(getLogoutUrl());
      setLogoutUrlPattern(logoutUrlPattern);
      setLocalLogout(localLogout);
      setCentralLogout(centralLogout);
    }

    @Override
    public void doFilter(
        final ServletRequest servletRequest,
        final ServletResponse servletResponse,
        final FilterChain filterChain)
        throws IOException, ServletException {

      CommonHelper.assertNotNull("logoutLogic", getLogoutLogic());
      CommonHelper.assertNotNull("config", getConfig());

      final HttpServletRequest request = (HttpServletRequest) servletRequest;
      final HttpServletResponse response = (HttpServletResponse) servletResponse;
      @SuppressWarnings("unchecked")
      final SessionStore<J2EContext> sessionStore = getConfig().getSessionStore();
      final J2EContext context =
          new J2EContext(
              request, response, sessionStore != null ? sessionStore : ShiroSessionStore.INSTANCE);

      // Destroy web session.
      getLogoutLogic()
          .perform(
              context,
              getConfig(),
              J2ENopHttpActionAdapter.INSTANCE,
              getDefaultUrl(),
              getLogoutUrlPattern(),
              getLocalLogout(),
              true,
              getCentralLogout());
    }
  }

  private static class AxelorCallbackFilter extends CallbackFilter {

    @Inject
    public AxelorCallbackFilter(Config config) {
      setConfig(config);

      final AppSettings settings = AppSettings.get();
      final String defaultUrl = settings.getBaseURL();

      if (StringUtils.notBlank(defaultUrl)) {
        setDefaultUrl(defaultUrl);
      }

      setDefaultClient(config.getClients().getClients().get(0).getName());
      setCallbackLogic(
          new ShiroCallbackLogic<Object, J2EContext>() {

            @Override
            public Object perform(
                J2EContext context,
                Config config,
                HttpActionAdapter<Object, J2EContext> httpActionAdapter,
                String inputDefaultUrl,
                Boolean inputSaveInSession,
                Boolean inputMultiProfile,
                Boolean inputRenewSession,
                String client) {

              try {
                context.getRequest().setCharacterEncoding("UTF-8");
              } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage(), e);
              }

              return super.perform(
                  context,
                  config,
                  httpActionAdapter,
                  inputDefaultUrl,
                  inputSaveInSession,
                  inputMultiProfile,
                  inputRenewSession,
                  client);
            }

            @SuppressWarnings("unchecked")
            @Override
            protected HttpAction redirectToOriginallyRequestedUrl(
                J2EContext context, String defaultUrl) {

              // Add CSRF token cookie and header
              AxelorCsrfTokenGeneratorAuthorizer csrfTokenAuthorizer =
                  (AxelorCsrfTokenGeneratorAuthorizer)
                      config.getAuthorizers().get(CSRF_TOKEN_AUTHORIZER_NAME);
              csrfTokenAuthorizer.addResponseCookieAndHeader(context);

              // if xhr, return status code only
              if (isXHR(context)) {
                return HttpAction.status(HttpConstants.OK, context);
              }

              final String requestedUrl =
                  (String) context.getSessionStore().get(context, Pac4jConstants.REQUESTED_URL);

              String redirectUrl = defaultUrl;
              if (StringUtils.notBlank(requestedUrl)) {
                context.getSessionStore().set(context, Pac4jConstants.REQUESTED_URL, null);
                redirectUrl = requestedUrl;
              }

              String hashLocation = context.getRequestParameter(HASH_LOCATION_PARAMETER);
              if (StringUtils.isBlank(hashLocation)) {
                hashLocation =
                    (String) context.getSessionStore().get(context, HASH_LOCATION_PARAMETER);
              }
              if (StringUtils.notBlank(hashLocation)) {
                redirectUrl = redirectUrl + hashLocation;
              }

              logger.debug("redirectUrl: {}", redirectUrl);
              return HttpAction.redirect(context, redirectUrl);
            }
          });
    }
  }

  private static class AxelorSecurityFilter extends SecurityFilter {

    @Inject
    public AxelorSecurityFilter(Config config) {
      setConfig(config);
      setAuthorizers(config.getAuthorizers().keySet().stream().collect(Collectors.joining(",")));

      final String clientNames =
          config.getClients().getClients().stream()
              .map(Client::getName)
              .collect(Collectors.joining(","));

      setClients(clientNames);

      setSecurityLogic(
          new ShiroSecurityLogic<Object, J2EContext>() {

            // Don't save requested URL if redirected to a non-default central client,
            // so that the requested URL saved before redirection will be used instead.
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            protected void saveRequestedUrl(J2EContext context, List<Client> currentClients) {
              final String hashLocation = context.getRequestParameter(HASH_LOCATION_PARAMETER);
              if (StringUtils.notBlank(hashLocation)) {
                context.getSessionStore().set(context, HASH_LOCATION_PARAMETER, hashLocation);
              }

              final String clientName =
                  context.getRequestParameter(Pac4jConstants.DEFAULT_CLIENT_NAME_PARAMETER);
              if (StringUtils.isBlank(clientName)
                  || currentClients.size() != 1
                  || !centralClientNames.contains(currentClients.get(0).getName())) {
                super.saveRequestedUrl(context, currentClients);
              }
            }
          });
    }
  }

  private static class AxelorLoginPageFilter implements Filter {

    @Inject private AxelorCallbackFilter axelorCallbackFilter;

    @Override
    public void init(javax.servlet.FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

      final Subject subject = SecurityUtils.getSubject();
      final boolean authenticated = subject.isAuthenticated();

      if (authenticated && AuthUtils.getUser() == null) {
        subject.logout();
      }

      // if already authenticated or if form login is not configured redirect to base url
      if (authenticated
          || AuthPac4jModule.clientList.stream()
              .noneMatch(client -> client instanceof FormClient)) {
        ((HttpServletResponse) response).sendRedirect(".");
        return;
      }

      // Perform callback filter if request body is not empty.
      if (request.getContentLengthLong() > 0L) {
        axelorCallbackFilter.doFilter(request, response, chain);
        return;
      }

      chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
  }
}
