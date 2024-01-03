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
package com.axelor.auth.pac4j.local;

import org.pac4j.core.credentials.UsernamePasswordCredentials;

public class AxelorFormCredentials extends UsernamePasswordCredentials {

  private static final long serialVersionUID = -651333458711052314L;

  private String newPassword;

  public AxelorFormCredentials(String username, String password, String newPassword) {
    super(username, password);
    this.newPassword = newPassword;
  }

  public AxelorFormCredentials(String username, String password) {
    this(username, password, null);
  }

  public String getNewPassword() {
    return newPassword;
  }
}
