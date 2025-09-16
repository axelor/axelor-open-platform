/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.i18n;

import com.axelor.app.internal.AppFilter;
import jakarta.validation.MessageInterpolator;
import java.util.Locale;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;

/**
 * This {@link MessageInterpolator} uses current user locale to translate hibernate validation
 * messages.
 */
public class I18nInterpolator extends ResourceBundleMessageInterpolator {

  private Locale getLocale() {
    Locale locale = AppFilter.getLocale();
    if (locale == null) {
      locale = Locale.getDefault();
    }
    return locale;
  }

  @Override
  public String interpolate(String message, Context context) {
    return super.interpolate(message, context, getLocale());
  }
}
