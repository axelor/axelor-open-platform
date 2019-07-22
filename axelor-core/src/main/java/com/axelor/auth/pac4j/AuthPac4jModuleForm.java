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
import com.axelor.auth.AuthService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.inject.Beans;
import javax.servlet.ServletContext;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.http.client.indirect.FormClient;

public class AuthPac4jModuleForm extends AuthPac4jModule {

  public static final String CONFIG_HTTP_FORM_ENABLED = "auth.http.form.enabled";

  private static final String INCORRECT_CREDENTIALS = /*$$(*/ "Wrong username or password"; /*)*/;
  private static final String WRONG_CURRENT_PASSWORD = /*$$(*/ "Wrong current password"; /*)*/;
  private static final String CHANGE_PASSWORD = /*$$(*/ "Please change your password." /*)*/;

  private static final String NEW_PASSWORD_PARAMETER = "newPassword";

  public AuthPac4jModuleForm(ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected final void configureClients() {
    final AppSettings settings = AppSettings.get();
    final boolean httpFormEnabled = settings.getBoolean(CONFIG_HTTP_FORM_ENABLED, true);

    if (httpFormEnabled) {
      final FormClient formClient = new Pac4jFormClient();
      addClient(formClient);
    }

    configureCentralClients();
  }

  protected void configureCentralClients() {
    // Nothing by default
  }

  @Override
  protected void configureAnon() {
    super.configureAnon();
    addFilterChain("/login.jsp", ANON);
    addFilterChain("/change-password.jsp", ANON);
  }

  private static class Pac4jFormClient extends FormClient {

    public Pac4jFormClient() {
      super("login.jsp", new Pac4jFormAuthenticator());
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

  private static class Pac4jFormAuthenticator
      implements Authenticator<UsernamePasswordCredentials> {

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

      if (user.getForcePasswordChange()) {
        if (StringUtils.isBlank(newPassword)) {
          throw new CredentialsException(CHANGE_PASSWORD);
        }

        JPA.runInTransaction(
            () -> {
              Beans.get(AuthService.class).changePassword(user, newPassword);
              user.setForcePasswordChange(false);
            });
      }

      final CommonProfile profile = new CommonProfile();
      profile.setId(username);
      profile.addAttribute(Pac4jConstants.USERNAME, username);
      credentials.setUserProfile(profile);
    }
  }
}
