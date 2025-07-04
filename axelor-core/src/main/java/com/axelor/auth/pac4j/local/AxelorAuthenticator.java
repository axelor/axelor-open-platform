/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import static com.axelor.auth.pac4j.AxelorProfileManager.AVAILABLE_MFA_METHODS;
import static com.axelor.auth.pac4j.AxelorProfileManager.FULLY_AUTHENTICATED;
import static com.axelor.auth.pac4j.AxelorProfileManager.PENDING_PROFILE;
import static com.axelor.auth.pac4j.AxelorProfileManager.PENDING_USER_NAME;

import com.axelor.auth.AuthService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.MFAService;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.google.inject.Inject;
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
import org.pac4j.core.profile.UserProfile;
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
  public static final String MISSING_2FA_USERNAME = /*$$(*/
      "Authentication session expired. Please log in again." /*)*/;
  public static final String INVALID_2FA_CODE = /*$$(*/
      "The verification code you entered is incorrect." /*)*/;
  @Inject MFAService mfaService;

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
    final String mfaCode =
        credentials instanceof AxelorFormCredentials axelorFormCredentials
            ? axelorFormCredentials.getMfaCode()
            : null;
    final String mfaMethod =
        credentials instanceof AxelorFormCredentials axelorFormCredentials
            ? axelorFormCredentials.getMfaMethod()
            : null;
    if (mfaCode != null && mfaMethod != null) {
      return validateMfaCode(ctx, mfaCode, mfaMethod);
    }
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

  private Optional<Credentials> validateMfaCode(CallContext ctx, String mfaCode, String mfaMethod) {

    if (StringUtils.isBlank(mfaCode) || StringUtils.isBlank(mfaMethod)) {
      throw new BadCredentialsException(INCOMPLETE_CREDENTIALS);
    }

    final var context = ctx.webContext();
    final var sessionStore = ctx.sessionStore();

    final var storedUsername =
        sessionStore
            .get(context, PENDING_USER_NAME)
            .map(Object::toString)
            .orElseThrow(() -> new CredentialsException(MISSING_2FA_USERNAME));

    final User user = AuthUtils.getUser(storedUsername);

    if (!mfaService.verifyCode(user, mfaCode, mfaMethod)) {
      throw new CredentialsException(INVALID_2FA_CODE);
    }

    sessionStore.set(context, PENDING_USER_NAME, null);
    sessionStore.set(context, AVAILABLE_MFA_METHODS, null);
    sessionStore.set(context, FULLY_AUTHENTICATED, true);

    UserProfile profile = null;
    var pendingProfile = sessionStore.get(context, PENDING_PROFILE);

    if (pendingProfile.isPresent()) {
      sessionStore.set(context, PENDING_PROFILE, null);
      if (pendingProfile.get() instanceof UserProfile userProfile) {
        profile = userProfile;
      }
    }

    if (profile == null) {
      profile = new CommonProfile();
      profile.setId(storedUsername);
      profile.addAttribute(Pac4jConstants.USERNAME, storedUsername);
    }

    final AxelorFormCredentials finalCredentials =
        new AxelorFormCredentials(storedUsername, null, null, mfaCode, mfaMethod);
    finalCredentials.setUserProfile(profile);

    return Optional.of(finalCredentials);
  }
}
