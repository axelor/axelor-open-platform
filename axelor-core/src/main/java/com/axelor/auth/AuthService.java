/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.auth.db.User;
import com.axelor.auth.pac4j.local.ChangePasswordException;
import com.axelor.auth.password.AuthPasswordManager;
import com.axelor.auth.password.policy.InvalidPolicy;
import com.axelor.auth.password.policy.PolicyDescription;
import com.axelor.inject.Beans;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.crypto.hash.format.ParsableHashFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AuthService} class provides various utility services including password encryption,
 * password match and saving user password in encrypted form.
 *
 * <p>The {@link AuthService} should not be manually instantiated but either injected or user {@link
 * #getInstance()} method to get the instance of the service.
 */
@Singleton
public class AuthService {

  protected static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final DefaultPasswordService passwordService = new DefaultPasswordService();

  private final ParsableHashFormat hashFormat =
      (ParsableHashFormat) passwordService.getHashFormat();

  @Inject private AuthPasswordManager passwordManager;

  /**
   * Get the instance of the {@link AuthService}.
   *
   * @throws IllegalStateException if AuthService is not initialized
   * @return the {@link AuthService} instance
   */
  public static AuthService getInstance() {
    try {
      return Beans.get(AuthService.class);
    } catch (Exception e) {
      throw new IllegalStateException(
          "AuthService is not initialized, did you forget to bind the AuthService?");
    }
  }

  /**
   * Encrypt the given password text if it's not encrypted yet.
   *
   * <p>The method tests the password for a special format to check if it is already encrypted, and
   * In that case the password is returned as it is to avoid multiple encryption.
   *
   * @param password the password to encrypt
   * @return encrypted password
   */
  public String encrypt(String password) {
    try {
      hashFormat.parse(password);
      return password;
    } catch (IllegalArgumentException e) {
      // ignore
    }
    return passwordService.encryptPassword(password);
  }

  /**
   * Encrypt the password of the given user.
   *
   * @param user the user whose password needs to be encrypted
   * @return the same user instance
   */
  public User encrypt(User user) {
    user.setPassword(encrypt(user.getPassword()));
    return user;
  }

  /**
   * This is an adapter method to be used with data import.
   *
   * <p>This method can be used as <code>call="com.axelor.auth.AuthService:encrypt"</code> while
   * importing user data to ensure user passwords are encrypted.
   *
   * @param user the object instance passed by data import engine
   * @param context the data import context
   * @return the same instance passed
   */
  public Object encrypt(Object user, @SuppressWarnings("rawtypes") Map context) {
    if (user instanceof User userInstance) {
      return encrypt(userInstance);
    }
    return user;
  }

  /**
   * Match the given plain and saved passwords.
   *
   * @param plain the plain password text
   * @param saved the saved password text (hashed)
   * @return true if they match
   */
  public boolean match(String plain, String saved) {
    return passwordService.passwordsMatch(plain, saved);
  }

  /**
   * Changes user password.
   *
   * @param user the user whose password needs to be changed
   * @param password the new plain-text password
   * @throws ChangePasswordException if unable to validate password policies
   */
  public void changePassword(User user, String password) {
    InvalidPolicy invalidPolicy = validatePasswordPolicies(user, password);
    if (invalidPolicy != null) {
      throw new ChangePasswordException(invalidPolicy);
    }

    user.setPassword(encrypt(password));
    user.setPasswordUpdatedOn(LocalDateTime.now());

    final User authUser = AuthUtils.getUser();

    // Update login date in session so that user changing own password doesn't get logged out.
    if (authUser != null && authUser.getId().equals(user.getId())) {
      Beans.get(AuthSessionService.class).updateLoginDate();
    }

    logger.debug("Password changed for user \"{}\"", user.getCode());
  }

  /**
   * Validates the given password against the configured password policies for the specified user.
   *
   * @param user the user for whom the password policies need to be validated
   * @param password the password to validate against the policies
   * @return {@link InvalidPolicy} if any policy is violated, null otherwise
   */
  public InvalidPolicy validatePasswordPolicies(User user, String password) {
    return passwordManager.validate(password, user);
  }

  /**
   * Returns the translated descriptions of all currently enabled password policies, in evaluation
   * order. Intended for display as requirements guidance on the login or change-password page.
   *
   * @return an ordered list of translated policy requirement strings
   */
  public List<String> getPasswordPolicyDescriptions() {
    return passwordManager.getDescriptions().stream()
        .map(PolicyDescription::getTranslatedMessage)
        .collect(Collectors.toList());
  }
}
