/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.identity;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.MFAService;
import com.axelor.auth.db.MFAMethod;
import com.axelor.auth.db.User;
import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.auth.pac4j.local.ChangePasswordException;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.exception.BadCredentialsException;
import org.pac4j.ldap.profile.service.LdapProfileService;

/**
 * Determines which identity verification method a user needs and performs the verification.
 *
 * <p>Verification methods depend on authentication context:
 *
 * <ul>
 *   <li>LDAP users: verify password against LDAP
 *   <li>Local users (with password): verify password against stored hash
 *   <li>External SSO/OAuth users (no password): verify via MFA
 * </ul>
 */
public class IdentityVerificationService {

  private final IdentityCheckService identityCheckService;
  private final AuthPac4jInfo authPac4jInfo;
  private final MFAService mfaService;

  @Inject
  public IdentityVerificationService(
      IdentityCheckService identityCheckService,
      AuthPac4jInfo authPac4jInfo,
      MFAService mfaService) {
    this.identityCheckService = identityCheckService;
    this.authPac4jInfo = authPac4jInfo;
    this.mfaService = mfaService;
  }

  /** Returns identity verification requirements for the current user. */
  public IdentityInfo getIdentityInfo() {
    User user = AuthUtils.getUser();
    if (user == null) {
      throw new IllegalStateException(I18n.get("No authenticated user"));
    }

    List<MFAMethod> mfaMethods = mfaService.getMethods(user);
    boolean requiresPassword = StringUtils.notEmpty(user.getPassword()) || isLdapConfigured();
    boolean requiresMfa = ObjectUtils.notEmpty(mfaMethods) && !requiresPassword;

    return new IdentityInfo(requiresPassword, requiresMfa, mfaMethods);
  }

  /**
   * Checks if the current user has any means to perform identity verification (if they have a
   * password or MFA enabled). If they can, then check if identity verification is currently
   * missing.
   *
   * @return true if identity verification is possible and missing
   */
  public boolean requiresIdentityCheck() {
    return canIdentityCheck() && !isIdentityChecked();
  }

  /**
   * Checks if the current user can perform identity verification.
   *
   * <p>The user must have either a password or MFA enabled.
   *
   * @return true if the current user can perform identity verification
   */
  protected boolean canIdentityCheck() {
    var identityInfo = getIdentityInfo();
    return identityInfo.requiresPassword() || identityInfo.requiresMfa();
  }

  /**
   * Checks whether the current session has a valid (non-expired) identity check.
   *
   * @return true if identity was checked within the last TTL minutes
   */
  protected boolean isIdentityChecked() {
    return identityCheckService.isIdentityChecked();
  }

  /** Marks the current session as identity-checked. */
  public void markIdentityChecked() {
    identityCheckService.markIdentityChecked();
  }

  /** Clears the identity check flag from the current session. */
  public void clearIdentityCheck() {
    identityCheckService.clearIdentityCheck();
  }

  /**
   * Verifies the user's identity based on the provided credentials.
   *
   * @param data credentials map (may contain "password" or "mfaCode"/"mfaMethod")
   * @throws IllegalArgumentException if verification fails
   */
  public void verifyIdentity(Map<String, Object> data) {
    User user = AuthUtils.getUser();
    if (user == null) {
      throw new IllegalStateException(I18n.get("No authenticated user"));
    }

    String password = (String) data.get("password");
    String mfaCode = (String) data.get("mfaCode");
    String mfaMethod = (String) data.get("mfaMethod");

    if (StringUtils.notBlank(password)) {
      verifyPassword(user, password);
    } else if (StringUtils.notBlank(mfaCode) && StringUtils.notBlank(mfaMethod)) {
      verifyMfa(user, mfaCode, mfaMethod);
    } else {
      throw new IllegalArgumentException(I18n.get("Unable to verify identity"));
    }

    identityCheckService.markIdentityChecked();
  }

  private void verifyPassword(User user, String password) {
    try {
      var credentials =
          authPac4jInfo
              .getAuthenticator()
              .validate(null, new UsernamePasswordCredentials(user.getCode(), password));
      if (!credentials.isEmpty()) {
        return;
      }
    } catch (BadCredentialsException e) {
      // Continue
    } catch (ChangePasswordException e) {
      // Ignore in case of `forcePasswordChange = true`
      return;
    }

    throw new IllegalArgumentException(I18n.get("Wrong password"));
  }

  private void verifyMfa(User user, String mfaCode, String mfaMethod) {
    if (!mfaService.verifyCode(user, mfaCode, mfaMethod)) {
      throw new IllegalArgumentException(I18n.get("Invalid verification code"));
    }
  }

  private boolean isLdapConfigured() {
    return authPac4jInfo.getAuthenticator() instanceof LdapProfileService;
  }
}
