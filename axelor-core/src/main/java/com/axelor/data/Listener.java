/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.data;

import com.axelor.db.Model;

/** Listener interface provides some events fired by Importer. */
public interface Listener {

  /**
   * Invoked when line is imported
   *
   * @param bean the bean instance mapped to the imported values
   */
  void imported(Model bean);

  /**
   * Invoked when file is imported
   *
   * @param total the total number of records processed
   * @param success the total number of records successfuly imported
   */
  void imported(Integer total, Integer success);

  /**
   * Invoked when bean failed managed
   *
   * @param bean the bean for which import failed
   * @param e the cause
   */
  void handle(Model bean, Exception e);
}
