/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.events;

public class LoginRedirectException extends RuntimeException {

  private static final long serialVersionUID = 4965839924701991583L;

  private final String location;

  public LoginRedirectException(String location) {
    super(String.format("Redirect: %s", location));
    this.location = location;
  }

  public String getLocation() {
    return location;
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
