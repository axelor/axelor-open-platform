/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import org.pac4j.core.credentials.UsernamePasswordCredentials;

public class AxelorFormCredentials extends UsernamePasswordCredentials {

  private static final long serialVersionUID = -651333458711052314L;

  private String newPassword;

  private String mfaCode;

  private String mfaMethod;

  public AxelorFormCredentials(
      String username, String password, String newPassword, String mfaCode, String mfaMethod) {
    super(username, password);
    this.newPassword = newPassword;
    this.mfaCode = mfaCode;
    this.mfaMethod = mfaMethod;
  }

  public AxelorFormCredentials(String username, String password) {
    this(username, password, null, null, null);
  }

  public String getNewPassword() {
    return newPassword;
  }

  public String getMfaCode() {
    return mfaCode;
  }

  public String getMfaMethod() {
    return mfaMethod;
  }
}
