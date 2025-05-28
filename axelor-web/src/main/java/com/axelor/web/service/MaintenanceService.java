/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.web.service;

import com.axelor.auth.db.User;
import com.google.inject.ImplementedBy;
import javax.servlet.http.HttpServletRequest;

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
