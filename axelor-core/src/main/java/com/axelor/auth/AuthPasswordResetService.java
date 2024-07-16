/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
