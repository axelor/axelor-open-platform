/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import static com.axelor.auth.pac4j.AxelorProfileManager.PENDING_USER_NAME;

import com.axelor.auth.AuthService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.auth.password.policy.InvalidPolicy;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.google.inject.Inject;
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

/**
 * Authenticator implementation for local username/password authentication.
 *
 * <p>This is the default authenticator used by the form and basic auth clients. It may also be
 * called outside the Pac4j flow, in which case the {@link CallContext} is {@code null}.
 *
 * <p>It also drives the password change flow: a user flagged with force password change must submit
 * a new password, and a submitted new password is stored only after it passes the password
 * policies.
 */
public class AxelorAuthenticator implements Authenticator {

  public static final String INCORRECT_CREDENTIALS = /*$$(*/ "Wrong username or password" /*)*/;
  public static final String NO_CREDENTIALS = "No credentials";
  public static final String UNSUPPORTED_CREDENTIALS = "Unsupported credentials";
  public static final String INCOMPLETE_CREDENTIALS = "Incomplete credentials";
  public static final String UNKNOWN_USER = "User doesn’t exist.";
  public static final String USER_DISABLED = "User is disabled.";
  public static final String WRONG_CURRENT_PASSWORD = /*$$(*/ "Wrong current password" /*)*/;

  @Inject AuthService authService;

  @Override
  public Optional<Credentials> validate(CallContext ctx, Credentials inputCredentials) {
    if (inputCredentials == null) {
      throw new BadCredentialsException(NO_CREDENTIALS);
    }

    if (!(inputCredentials instanceof UsernamePasswordCredentials)) {
      throw new BadCredentialsException(UNSUPPORTED_CREDENTIALS);
    }

    final UsernamePasswordCredentials credentials = (UsernamePasswordCredentials) inputCredentials;

    final String username = credentials.getUsername();
    final String password = credentials.getPassword();
    final String newPassword =
        credentials instanceof AxelorFormCredentials axelorFormCredentials
            ? axelorFormCredentials.getNewPassword()
            : null;

    final boolean passwordChangeRequest = StringUtils.notBlank(newPassword);

    // Validate the credentials and get the matching user
    final User user = validateUser(username, password, passwordChangeRequest);

    // Force the password change even if the user didn't provide a new one
    if (Boolean.TRUE.equals(user.getForcePasswordChange()) && !passwordChangeRequest) {
      throw new ChangePasswordException();
    }

    // Change the user password
    if (passwordChangeRequest) {

      // Check the policies to validate the new password
      InvalidPolicy invalidPolicy = authService.validatePasswordPolicies(user, newPassword);
      if (invalidPolicy != null) {
        throw new ChangePasswordException(invalidPolicy);
      }

      // Store the new password
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

    // Authenticator may be used outside of Pac4j flow
    if (ctx != null) {
      final var context = ctx.webContext();
      final var sessionStore = ctx.sessionStore();

      // Prevent unwanted session creation.
      if (sessionStore.getSessionId(context, false).isPresent()) {
        sessionStore.set(context, PENDING_USER_NAME, null);
      }
    }

    // Create the profile
    final CommonProfile profile = new CommonProfile();
    profile.setId(user.getCode());
    profile.addAttribute(Pac4jConstants.USERNAME, user.getCode());
    credentials.setUserProfile(profile);

    return Optional.of(credentials);
  }

  /**
   * Finds the user matching the given credentials.
   *
   * @param username the username
   * @param password the current password
   * @param passwordChangeRequest whether the user is changing their password
   * @return the matching active user
   * @throws CredentialsException if the credentials are incomplete or don't match an active user
   */
  protected User validateUser(String username, String password, boolean passwordChangeRequest) {
    // Check if credentials are provided
    if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
      throw new BadCredentialsException(INCOMPLETE_CREDENTIALS);
    }

    // Check if the user exists
    final User user = AuthUtils.getUser(username);
    if (user == null) {
      throw new AccountNotFoundException(UNKNOWN_USER);
    }

    // Check if the user is active
    if (!AuthUtils.isActive(user)) {
      throw new AccountNotFoundException(USER_DISABLED);
    }

    // Check the password match
    if (user.getPassword() == null || !authService.match(password, user.getPassword())) {
      throw new BadCredentialsException(
          passwordChangeRequest ? WRONG_CURRENT_PASSWORD : INCORRECT_CREDENTIALS);
    }

    return user;
  }
}
