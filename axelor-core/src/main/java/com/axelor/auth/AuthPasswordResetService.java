/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.mail.MailException;
import com.google.inject.ImplementedBy;

@ImplementedBy(AuthPasswordResetServiceImpl.class)
public interface AuthPasswordResetService {

  /**
   * Checks whether password reset service is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Submits forgot password.
   *
   * <p>Consumes existing password reset tokens for user and sends a password reset email.
   *
   * @param emailAddress the email address of the user
   * @throws MailException on email sending failure
   */
  void submitForgotPassword(String emailAddress) throws MailException;

  /**
   * Checks whether the token is valid.
   *
   * @param token the password reset token
   * @throws IllegalArgumentException if the token is invalid
   */
  void checkToken(String token);

  /**
   * Changes the user password and marks the token as consumed.
   *
   * @param token the password reset token
   * @param password the new password
   * @throws IllegalArgumentException if the token is invalid
   * @throws IllegalArgumentException if the password is invalid
   */
  void changePassword(String token, String password);
}
