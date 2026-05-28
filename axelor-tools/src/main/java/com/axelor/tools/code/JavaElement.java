/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code;

/** Interface for all code elements that can emit some code. */
public interface JavaElement {

  /**
   * Emit the code.
   *
   * @param writer the pojo writer
   */
  void emit(JavaWriter writer);
}
