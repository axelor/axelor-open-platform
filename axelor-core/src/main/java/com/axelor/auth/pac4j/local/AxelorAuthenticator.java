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
package com.axelor.auth.pac4j.local;

import com.axelor.auth.AuthService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import java.util.Objects;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.AccountNotFoundException;
import org.pac4j.core.exception.BadCredentialsException;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.Pac4jConstants;

public class AxelorAuthenticator implements Authenticator {

  public static final String INCORRECT_CREDENTIALS = /*$$(*/ "Wrong username or password" /*)*/;
  public static final String NO_CREDENTIALS = "No credentials";
  public static final String INCOMPLETE_CREDENTIALS = "Incomplete credentials";
  public static final String UNKNOWN_USER = "User doesn’t exist.";
  public static final String USER_DISABLED = "User is disabled.";
  public static final String WRONG_CURRENT_PASSWORD = /*$$(*/ "Wrong current password" /*)*/;
  public static final String NEW_PASSWORD_MUST_BE_DIFFERENT = /*$$(*/
      "New password must be different." /*)*/;
  public static final String NEW_PASSWORD_DOES_NOT_MATCH_PATTERN = /*$$(*/
      "New password does not match pattern." /*)*/;

  @Override
  public void validate(
      Credentials inputCredentials, WebContext context, SessionStore sessionStore) {
    final AxelorFormCredentials credentials = (AxelorFormCredentials) inputCredentials;

    if (credentials == null) {
      throw new BadCredentialsException(NO_CREDENTIALS);
    }

    final String username = credentials.getUsername();
    final String password = credentials.getPassword();
    final String newPassword = credentials.getNewPassword();

    if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
      throw new BadCredentialsException(INCOMPLETE_CREDENTIALS);
    }

    final User user = AuthUtils.getUser(username);

    if (user == null) {
      throw new AccountNotFoundException(UNKNOWN_USER);
    }

    if (!AuthUtils.isActive(user)) {
      throw new AccountNotFoundException(USER_DISABLED);
    }

    final AuthService authService = AuthService.getInstance();

    if (!authService.match(password, user.getPassword())) {
      if (StringUtils.isBlank(newPassword)) {
        throw new BadCredentialsException(INCORRECT_CREDENTIALS);
      }

      throw new BadCredentialsException(WRONG_CURRENT_PASSWORD);
    }

    final boolean hasNewPassword = StringUtils.notBlank(newPassword);
    final boolean forcePasswordChange = Boolean.TRUE.equals(user.getForcePasswordChange());

    if (hasNewPassword || forcePasswordChange) {
      if (!hasNewPassword) {
        throw new ChangePasswordException();
      }

      if (Objects.equals(newPassword, password)) {
        throw new ChangePasswordException(NEW_PASSWORD_MUST_BE_DIFFERENT);
      }

      if (!authService.passwordMatchesPattern(newPassword)) {
        throw new ChangePasswordException(NEW_PASSWORD_DOES_NOT_MATCH_PATTERN);
      }

      JPA.runInTransaction(
          () -> {
            try {
              authService.changePassword(user, newPassword);
              user.setForcePasswordChange(false);
            } catch (Exception e) {
              throw new CredentialsException(e.getMessage());
            }
          });
    }

    final CommonProfile profile = new CommonProfile();
    profile.setId(username);
    profile.addAttribute(Pac4jConstants.USERNAME, username);
    credentials.setUserProfile(profile);
  }
}
