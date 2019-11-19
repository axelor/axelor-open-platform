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
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.AuthService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.inject.Beans;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import javax.servlet.ServletContext;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.credentials.extractor.FormExtractor;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.http.ajax.DefaultAjaxRequestResolver;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.redirect.RedirectAction;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.indirect.FormClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthPac4jModuleLocal extends AuthPac4jModule {

  private static final String INCORRECT_CREDENTIALS = /*$$(*/ "Wrong username or password" /*)*/;
  private static final String WRONG_CURRENT_PASSWORD = /*$$(*/ "Wrong current password" /*)*/;
  private static final String CHANGE_PASSWORD = /*$$(*/ "Please change your password." /*)*/;

  private static final String NEW_PASSWORD_PARAMETER = "newPassword";

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public AuthPac4jModuleLocal(ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected void configureClients() {
    addLocalClients();
  }

  @Override
  protected void configureAnon() {
    super.configureAnon();
    addFilterChain("/login.jsp", ANON);
    addFilterChain("/change-password.jsp", ANON);
  }

  protected void addLocalClients() {
    final AppSettings settings = AppSettings.get();
    final String ldapUrl = settings.get(AvailableAppSettings.AUTH_LDAP_SERVER_URL, null);

    if (ldapUrl != null) {
      bind(AxelorAuthenticator.class).to(AxelorLdapProfileService.class);
      logger.info("LDAP URL: {}", ldapUrl);
    } else {
      bind(AxelorAuthenticator.class).to(AxelorLocalAuthenticator.class);
    }

    expose(AxelorAuthenticator.class);
    addLocalClient(new AxelorFormClient());

    if (isBasicAuthEnabled()) {
      addLocalClient(new AxelorDirectBasicAuthClient());
    }
  }

  public static boolean isLdapEnabled() {
    final AppSettings settings = AppSettings.get();
    return settings.get(AvailableAppSettings.AUTH_LDAP_SERVER_URL, null) != null;
  }

  public static boolean isBasicAuthEnabled() {
    final AppSettings settings = AppSettings.get();
    return settings.getBoolean(AvailableAppSettings.AUTH_LOCAL_BASIC_AUTH_ENABLED, false);
  }

  private static class AxelorFormClient extends FormClient {

    public AxelorFormClient() {
      setName(getClass().getSuperclass().getSimpleName());
      setCredentialsExtractor(new JsonExtractor());
      setAjaxRequestResolver(new AxelorAjaxRequestResolver());
    }

    @Override
    protected void clientInit() {
      final String baseUrl = AuthPac4jModule.getRelativeBaseURL();
      final String loginUrl = baseUrl + "/login.jsp";
      setLoginUrl(loginUrl);

      final Authenticator<UsernamePasswordCredentials> authenticator =
          Beans.get(AxelorAuthenticator.class);
      defaultAuthenticator(authenticator);

      super.clientInit();
    }

    @Override
    protected UsernamePasswordCredentials retrieveCredentials(final WebContext context) {
      CommonHelper.assertNotNull("credentialsExtractor", getCredentialsExtractor());
      CommonHelper.assertNotNull("authenticator", getAuthenticator());

      String username = context.getRequestParameter(getUsernameParameter());
      final UsernamePasswordCredentials credentials;
      try {
        // retrieve credentials
        credentials = getCredentialsExtractor().extract(context);
        logger.debug("usernamePasswordCredentials: {}", credentials);
        if (credentials == null) {
          throw handleInvalidCredentials(
              context,
              username,
              "Username and password cannot be blank -> return to the form with error",
              MISSING_FIELD_ERROR);
        }
        // username in AJAX request
        if (username == null) {
          username = credentials.getUsername();
        }
        // validate credentials
        getAuthenticator().validate(credentials, context);
      } catch (final CredentialsException e) {
        throw handleInvalidCredentials(
            context,
            username,
            "Credentials validation fails -> return to the form with error",
            computeErrorMessage(e));
      }

      return credentials;
    }

    @Override
    protected String computeErrorMessage(Exception e) {
      return e.getMessage();
    }

    @Override
    protected HttpAction handleInvalidCredentials(
        WebContext context, String username, String message, String errorMessage) {

      if (CHANGE_PASSWORD.equals(errorMessage)
          || StringUtils.notBlank(context.getRequestParameter(NEW_PASSWORD_PARAMETER))) {
        String redirectionUrl =
            CommonHelper.addParameter("change-password.jsp", getUsernameParameter(), username);
        redirectionUrl = CommonHelper.addParameter(redirectionUrl, ERROR_PARAMETER, errorMessage);
        return HttpAction.redirect(context, redirectionUrl);
      }

      logger.error("Password authentication failed for user: {}", username);
      return super.handleInvalidCredentials(context, username, message, errorMessage);
    }
  }

  private static class AxelorDirectBasicAuthClient extends DirectBasicAuthClient {
    public AxelorDirectBasicAuthClient() {
      setName(getClass().getSuperclass().getSimpleName());
    }

    @Override
    protected void clientInit() {
      final Authenticator<UsernamePasswordCredentials> authenticator =
          Beans.get(AxelorAuthenticator.class);
      defaultAuthenticator(authenticator);
      super.clientInit();
    }

    @Override
    protected UsernamePasswordCredentials retrieveCredentials(final WebContext context) {
      if (StringUtils.isBlank(context.getRequestHeader(HttpConstants.AUTHORIZATION_HEADER))) {
        return null;
      }
      return super.retrieveCredentials(context);
    }
  }

  public static interface AxelorAuthenticator extends Authenticator<UsernamePasswordCredentials> {}

  public static class AxelorLocalAuthenticator implements AxelorAuthenticator {

    @Override
    public void validate(UsernamePasswordCredentials credentials, WebContext context) {
      if (credentials == null) {
        throw new CredentialsException("No credentials");
      }

      final String username = credentials.getUsername();
      final String password = credentials.getPassword();

      if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
        throw new CredentialsException(INCORRECT_CREDENTIALS);
      }

      final User user = AuthUtils.getUser(username);

      if (user == null) {
        throw new CredentialsException(INCORRECT_CREDENTIALS);
      }

      final AuthService authService = AuthService.getInstance();
      final String newPassword = context.getRequestParameter(NEW_PASSWORD_PARAMETER);

      if (!authService.match(password, user.getPassword())) {
        if (StringUtils.isBlank(newPassword)) {
          throw new CredentialsException(INCORRECT_CREDENTIALS);
        }

        throw new CredentialsException(WRONG_CURRENT_PASSWORD);
      }

      if (Boolean.TRUE.equals(user.getForcePasswordChange())) {
        if (StringUtils.isBlank(newPassword)) {
          throw new CredentialsException(CHANGE_PASSWORD);
        }

        JPA.runInTransaction(
            () -> {
              try {
                Beans.get(AuthService.class).changePassword(user, newPassword);
              } catch (IllegalArgumentException e) {
                throw new CredentialsException(e.getMessage());
              }
              user.setForcePasswordChange(false);
            });
      }

      final CommonProfile profile = new CommonProfile();
      profile.setId(username);
      profile.addAttribute(Pac4jConstants.USERNAME, username);
      credentials.setUserProfile(profile);
    }
  }

  private static class AxelorAjaxRequestResolver extends DefaultAjaxRequestResolver {

    @Override
    public boolean isAjax(WebContext context) {
      return super.isAjax(context) || isXHR(context);
    }

    @Override
    public RedirectAction buildAjaxResponse(String url, WebContext context) {
      if (isAjax(context) && StringUtils.notBlank(url) && url.equals("login.jsp")) {
        throw HttpAction.unauthorized(context);
      }
      return super.buildAjaxResponse(url, context);
    }
  }

  private static class JsonExtractor extends FormExtractor {

    public JsonExtractor() {
      super(Pac4jConstants.USERNAME, Pac4jConstants.PASSWORD);
    }

    @Override
    public UsernamePasswordCredentials extract(WebContext context) {
      return isXHR(context) ? extractJson(context) : super.extract(context);
    }

    private UsernamePasswordCredentials extractJson(WebContext context) {
      final Map<?, ?> data;
      try {
        data = new ObjectMapper().readValue(context.getRequestContent(), Map.class);
      } catch (IOException e) {
        return null;
      }

      final String username = (String) data.get("username");
      final String password = (String) data.get("password");
      if (username == null || password == null) {
        return null;
      }

      return new UsernamePasswordCredentials(username, password);
    }
  }
}
