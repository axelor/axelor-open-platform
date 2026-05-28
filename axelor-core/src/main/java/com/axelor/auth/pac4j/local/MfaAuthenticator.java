/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import static com.axelor.auth.pac4j.AxelorProfileManager.AVAILABLE_MFA_METHODS;
import static com.axelor.auth.pac4j.AxelorProfileManager.FULLY_AUTHENTICATED;
import static com.axelor.auth.pac4j.AxelorProfileManager.PENDING_PROFILE;
import static com.axelor.auth.pac4j.AxelorProfileManager.PENDING_USER_NAME;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.MFAService;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.Pac4jConstants;

@Singleton
public class MfaAuthenticator implements Authenticator {

  public static final String MISSING_MFA_USERNAME = /*$$(*/
      "Authentication session expired. Please log in again." /*)*/;
  public static final String INVALID_MFA_CODE = /*$$(*/
      "The verification code you entered is incorrect." /*)*/;

  private final MFAService mfaService;

  @Inject
  public MfaAuthenticator(MFAService mfaService) {
    this.mfaService = mfaService;
  }

  @Override
  public Optional<Credentials> validate(CallContext ctx, Credentials credentials) {
    return getMfaCredentials(credentials).flatMap(mfaCredentials -> validate(ctx, mfaCredentials));
  }

  public Optional<Credentials> validate(CallContext ctx, AxelorFormCredentials mfaCredentials) {
    var mfaCode = mfaCredentials.getMfaCode();
    var mfaMethod = mfaCredentials.getMfaMethod();

    final var context = ctx.webContext();
    final var sessionStore = ctx.sessionStore();

    final var storedUsername =
        sessionStore
            .get(context, PENDING_USER_NAME)
            .map(Object::toString)
            .orElseThrow(() -> new CredentialsException(MISSING_MFA_USERNAME));

    final User user = AuthUtils.getUser(storedUsername);

    if (!mfaService.verifyCode(user, mfaCode, mfaMethod)) {
      throw new CredentialsException(INVALID_MFA_CODE);
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

  public Optional<AxelorFormCredentials> getMfaCredentials(Credentials credentials) {
    return Optional.ofNullable(credentials)
        .filter(AxelorFormCredentials.class::isInstance)
        .map(AxelorFormCredentials.class::cast)
        .filter(
            formCredentials ->
                StringUtils.notBlank(formCredentials.getMfaCode())
                    && StringUtils.notBlank(formCredentials.getMfaMethod()));
  }
}
