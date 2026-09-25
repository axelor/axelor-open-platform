/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import com.axelor.common.StringUtils;
import java.util.Map;

/**
 * Custom converter for logback that outputs the current tenant identifier.
 *
 * <p>The tenant is read from the MDC key {@link LoggerConfiguration#TENANT_MDC_KEY}. A fallback can
 * be given as option, used when no tenant is set on the thread, e.g. <code>%tenant{-}</code>. If
 * there is no tenant and no fallback, nothing is output.
 */
public class TenantConverter extends ClassicConverter {

  private String fallback;

  @Override
  public void start() {
    fallback = getFirstOption();
    super.start();
  }

  @Override
  public String convert(ILoggingEvent event) {
    final Map<String, String> mdc = event.getMDCPropertyMap();
    final String tenant = mdc == null ? null : mdc.get(LoggerConfiguration.TENANT_MDC_KEY);
    if (StringUtils.notBlank(tenant)) {
      return tenant;
    }
    return fallback == null ? CoreConstants.EMPTY_STRING : fallback;
  }
}
