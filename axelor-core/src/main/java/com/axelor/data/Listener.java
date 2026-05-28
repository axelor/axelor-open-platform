/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
