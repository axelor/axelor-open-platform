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
import java.lang.invoke.MethodHandles;
import javax.servlet.ServletContext;
import org.jasig.cas.client.validation.TicketValidator;
import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.client.CasProxyReceptor;
import org.pac4j.cas.client.direct.DirectCasClient;
import org.pac4j.cas.client.direct.DirectCasProxyClient;
import org.pac4j.cas.client.rest.CasRestBasicAuthClient;
import org.pac4j.cas.client.rest.CasRestFormClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.cas.config.CasProtocol;
import org.pac4j.core.client.Client;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.http.url.UrlResolver;
import org.pac4j.core.logout.handler.LogoutHandler;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthPac4jModuleCas extends AuthPac4jModule {

  // Application configuration
  public static final String CONFIG_CAS_LOGIN_URL = "auth.cas.login.url";
  public static final String CONFIG_CAS_PREFIX_URL = "auth.cas.prefix.url";
  public static final String CONFIG_CAS_PROTOCOL = "auth.cas.protocol";

  public static final String CONFIG_CAS_ENCODING = "auth.cas.encoding";
  public static final String CONFIG_CAS_RENEW = "auth.cas.renew";
  public static final String CONFIG_CAS_GATEWAY = "auth.cas.gateway";
  public static final String CONFIG_CAS_TIME_TOLERANCE = "auth.cas.time.tolerance";
  public static final String CONFIG_CAS_TIME_TICKET_VALIDATOR = "auth.cas.ticket.validator";
  public static final String CONFIG_CAS_URL_RESOLVER_CLASS = "auth.cas.url.resolver.class";
  public static final String CONFIG_CAS_DEFAULT_TICKET_VALIDATOR_CLASS =
      "auth.cas.default.ticket.validator.class";

  public static final String CONFIG_CAS_PROXY_SUPPORT = "auth.cas.proxy.support";
  public static final String CONFIG_CAS_LOGOUT_HANDLER_CLASS = "auth.cas.logout.handler.class";

  // CasClient / DirectCasClient / DirectCasProxyClient / CasRestFormClient / CasRestBasicAuthClient
  public static final String CONFIG_CAS_CLIENT_NAME = "auth.cas.client.name";

  // Configuration for DirectCasProxyClient
  public static final String CONFIG_CAS_SERVICE_URL = "auth.cas.service.url";

  // Configuration for CasRestFormClient
  public static final String CONFIG_CAS_USERNAME_PARAMETER = "auth.cas.username.parameter";
  public static final String CONFIG_CAS_PASSWORD_PARAMETER = "auth.cas.password.parameter";

  // Configuration for CasRestBasicAuthClient
  public static final String CONFIG_CAS_HEADER_NAME = "auth.cas.header.name";
  public static final String CONFIG_CAS_PREFIX_HEADER = "auth.cas.prefix.header";

  // Backward-compatible CAS configuration
  public static final String CONFIG_CAS_SERVER_PREFIX_URL = "auth.cas.server.url.prefix";
  public static final String CONFIG_CAS_SERVICE = "auth.cas.service";
  public static final String CONFIG_CAS_LOGOUT_URL = "auth.cas.logout.url";

  public static final String CONFIG_CAS_ATTRS_USER_NAME = "auth.cas.attrs.user.name";
  public static final String CONFIG_CAS_ATTRS_USER_EMAIL = "auth.cas.attrs.user.email";

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public AuthPac4jModuleCas(ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected void configureClients() {
    final AppSettings settings = AppSettings.get();
    final String loginUrl = settings.get(CONFIG_CAS_LOGIN_URL);
    final String prefixUrl =
        settings.get(CONFIG_CAS_PREFIX_URL, settings.get(CONFIG_CAS_SERVER_PREFIX_URL, null));
    final String protocol = settings.get(CONFIG_CAS_PROTOCOL, null);

    final String encoding = settings.get(CONFIG_CAS_ENCODING, null);
    final boolean renew = settings.getBoolean(CONFIG_CAS_RENEW, false);
    final boolean gateway = settings.getBoolean(CONFIG_CAS_GATEWAY, false);
    final long timeTolerance = settings.getInt(CONFIG_CAS_TIME_TOLERANCE, 1000);
    final String urlResolverClass = settings.get(CONFIG_CAS_URL_RESOLVER_CLASS, null);
    final String defaultTicketValidatorClass =
        settings.get(CONFIG_CAS_DEFAULT_TICKET_VALIDATOR_CLASS, null);

    final boolean proxySupport = settings.getBoolean(CONFIG_CAS_PROXY_SUPPORT, false);
    final String logoutHandlerClass = settings.get(CONFIG_CAS_LOGOUT_HANDLER_CLASS, null);
    final String serviceUrl = settings.get(CONFIG_CAS_SERVICE_URL, null);
    final String clientName = settings.get(CONFIG_CAS_CLIENT_NAME, "CasClient");

    final String usernameParameter = settings.get(CONFIG_CAS_USERNAME_PARAMETER, "username");
    final String passwordParameter = settings.get(CONFIG_CAS_PASSWORD_PARAMETER, "password");

    final String headerName = settings.get(CONFIG_CAS_HEADER_NAME, "Authorization");
    final String prefixHeader = settings.get(CONFIG_CAS_PREFIX_HEADER, "Basic ");

    final CasConfiguration casConfig = new CasConfiguration(loginUrl);

    if (prefixUrl != null) {
      casConfig.setPrefixUrl(prefixUrl);
    }

    if (protocol != null) {
      casConfig.setProtocol(CasProtocol.valueOf(protocol));
    }

    if (encoding != null) {
      casConfig.setEncoding(encoding);
    }

    casConfig.setRenew(renew);
    casConfig.setGateway(gateway);
    casConfig.setTimeTolerance(timeTolerance);

    if (urlResolverClass != null) {
      try {
        final UrlResolver urlResorver = (UrlResolver) Class.forName(urlResolverClass).newInstance();
        casConfig.setUrlResolver(urlResorver);
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
        logger.error(e.getMessage(), e);
      }
    }

    if (defaultTicketValidatorClass != null) {
      try {
        final TicketValidator defaultTicketValidator =
            (TicketValidator) Class.forName(defaultTicketValidatorClass).newInstance();
        casConfig.setDefaultTicketValidator(defaultTicketValidator);
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
        logger.error(e.getMessage(), e);
      }
    }

    if (proxySupport) {
      final CasProxyReceptor casProxy = new CasProxyReceptor();
      casConfig.setProxyReceptor(casProxy);
    }

    if (logoutHandlerClass != null) {
      try {
        @SuppressWarnings("unchecked")
        final LogoutHandler<? extends WebContext> logoutHandler =
            (LogoutHandler<? extends WebContext>) Class.forName(logoutHandlerClass).newInstance();
        casConfig.setLogoutHandler(logoutHandler);
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
        logger.error(e.getMessage(), e);
      }
    }

    final Client<? extends Credentials, ? extends CommonProfile> client;
    switch (clientName) {
      case "DirectCasClient":
        client = new DirectCasClient(casConfig);
        break;
      case "DirectCasProxyClient":
        client = new DirectCasProxyClient(casConfig, serviceUrl);
        break;
      case "CasRestFormClient":
        client = new CasRestFormClient(casConfig, usernameParameter, passwordParameter);
        break;
      case "CasRestBasicAuthClient":
        client = new CasRestBasicAuthClient(casConfig, headerName, prefixHeader);
        break;
      case "CasClient":
      default:
        client = new CasClient(casConfig);
    }

    addClient(client);
  }

  public static boolean isEnabled() {
    final AppSettings settings = AppSettings.get();
    return settings.get(CONFIG_CAS_LOGIN_URL, null) != null;
  }
}
