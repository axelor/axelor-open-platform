/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.quartz;

import com.google.inject.AbstractModule;
import org.quartz.Scheduler;

/** The default guice module for quartz scheduler. */
public class SchedulerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Scheduler.class).toProvider(SchedulerProvider.class);
  }
}
