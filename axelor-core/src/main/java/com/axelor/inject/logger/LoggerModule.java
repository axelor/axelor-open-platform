/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.inject.logger;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import org.slf4j.Logger;

/** Provides support for SLF4J Logger injection. */
public final class LoggerModule extends AbstractModule {

  @Override
  protected void configure() {
    bindListener(Matchers.any(), new LoggerProvisionListener());
    bind(Logger.class).toProvider(new LoggerProvider());
  }
}
