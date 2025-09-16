/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.service;

import com.axelor.auth.db.User;
import com.google.inject.ImplementedBy;
import jakarta.servlet.http.HttpServletRequest;

/** Service for managing maintenance mode */
@ImplementedBy(MaintenanceServiceNever.class)
public interface MaintenanceService {

  /**
   * Returns whether maintenance mode is enabled.
   *
   * @param user the current user or null
   * @param httpRequest the current http request
   * @return true if maintenance mode is enabled
   */
  boolean isMaintenanceMode(User user, HttpServletRequest httpRequest);
}
