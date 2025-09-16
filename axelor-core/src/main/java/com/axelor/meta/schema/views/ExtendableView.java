/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import java.util.List;

public interface ExtendableView {
  Boolean getExtension();

  List<Extend> getExtends();

  void setExtends(List<Extend> extendItems);
}
