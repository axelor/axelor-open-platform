/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.text;

import com.axelor.db.Model;
import java.util.Map;

/** The {@link Template} interface defines methods to render the template. */
public interface Template {

  /**
   * Make a template renderer using the given context.
   *
   * @param context the template context
   * @return a {@link Renderer} instance
   */
  Renderer make(Map<String, Object> context);

  /**
   * Make a template renderer using the given context.
   *
   * @param <T> type of the context bean
   * @param context the template context
   * @return a {@link Renderer} instance
   */
  <T extends Model> Renderer make(T context);
}
