/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import static com.axelor.auth.pac4j.AxelorProfileManager.PENDING_USER_NAME;

import com.axelor.auth.AuthService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.Optional;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.AccountNotFoundException;
import org.pac4j.core.exception.BadCredentialsException;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.Pac4jConstants;

public class AxelorAuthenticator implements Authenticator {

  public static final String INCORRECT_CREDENTIALS = /*$$(*/ "Wrong username or password" /*)*/;
  public static final String NO_CREDENTIALS = "No credentials";
  public static final String UNSUPPORTED_CREDENTIALS = "Unsupported credentials";
  public static final String INCOMPLETE_CREDENTIALS = "Incomplete credentials";
  public static final String UNKNOWN_USER = "User doesn’t exist.";
  public static final String USER_DISABLED = "User is disabled.";
  public static final String WRONG_CURRENT_PASSWORD = /*$$(*/ "Wrong current password" /*)*/;
  public static final String NEW_PASSWORD_MUST_BE_DIFFERENT = /*$$(*/
      "New password must be different." /*)*/;
  public static final String NEW_PASSWORD_DOES_NOT_MATCH_PATTERN = /*$$(*/
      "New password does not match pattern." /*)*/;

  private final MfaAuthenticator mfaAuthenticator;

  @Inject
  public AxelorAuthenticator(MfaAuthenticator mfaAuthenticator) {
    this.mfaAuthenticator = mfaAuthenticator;
  }

  @Override
  public Optional<Credentials> validate(CallContext ctx, Credentials inputCredentials) {
    if (inputCredentials == null) {
      throw new BadCredentialsException(NO_CREDENTIALS);
    }

    if (!(inputCredentials instanceof UsernamePasswordCredentials)) {
      throw new BadCredentialsException(UNSUPPORTED_CREDENTIALS);
    }

    final UsernamePasswordCredentials credentials = (UsernamePasswordCredentials) inputCredentials;

    var mfaCredentials = mfaAuthenticator.getMfaCredentials(credentials);
    if (mfaCredentials.isPresent()) {
      return mfaAuthenticator.validate(ctx, mfaCredentials.get());
    }

    final String username = credentials.getUsername();
    final String password = credentials.getPassword();
    final String newPassword =
        credentials instanceof AxelorFormCredentials axelorFormCredentials
            ? axelorFormCredentials.getNewPassword()
            : null;

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

    final var context = ctx.webContext();
    final var sessionStore = ctx.sessionStore();

    sessionStore.set(context, PENDING_USER_NAME, null);

    final CommonProfile profile = new CommonProfile();
    profile.setId(username);
    profile.addAttribute(Pac4jConstants.USERNAME, username);
    credentials.setUserProfile(profile);

    return Optional.of(credentials);
  }
}
