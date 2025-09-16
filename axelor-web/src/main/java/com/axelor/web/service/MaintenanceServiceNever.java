/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.service;

import com.axelor.auth.db.User;
import jakarta.servlet.http.HttpServletRequest;

/** Default maintenance service that never turns on maintenance mode */
public class MaintenanceServiceNever implements MaintenanceService {

  @Override
  public boolean isMaintenanceMode(User user, HttpServletRequest httpRequest) {
    return false;
  }
}
