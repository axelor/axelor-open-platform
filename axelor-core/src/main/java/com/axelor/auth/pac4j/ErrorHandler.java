/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j;

import java.lang.invoke.MethodHandles;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandler {

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void handleException(
      Exception e, HttpActionAdapter httpActionAdapter, WebContext context) {
    logger.error(e.getMessage());
    logger.debug(e.getMessage(), e);
  }
}
