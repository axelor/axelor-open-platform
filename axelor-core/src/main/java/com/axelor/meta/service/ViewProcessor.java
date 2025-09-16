/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.service;

import com.axelor.meta.schema.views.AbstractView;

public interface ViewProcessor {

  void process(AbstractView view);
}
