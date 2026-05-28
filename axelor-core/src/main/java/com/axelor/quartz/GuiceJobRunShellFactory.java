/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.quartz;

import org.quartz.Scheduler;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.core.JobRunShell;
import org.quartz.core.JobRunShellFactory;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.spi.TriggerFiredBundle;

/** Custom {@link JobRunShellFactory} to use {@link GuiceJobRunShell}. */
public class GuiceJobRunShellFactory implements JobRunShellFactory {

  private Scheduler scheduler;
  private QuartzSchedulerResources resources;

  public GuiceJobRunShellFactory(QuartzSchedulerResources resources) {
    this.resources = resources;
  }

  @Override
  public void initialize(Scheduler scheduler) throws SchedulerConfigException {
    this.scheduler = scheduler;
  }

  @Override
  public JobRunShell createJobRunShell(TriggerFiredBundle bundle) throws SchedulerException {
    return new GuiceJobRunShell(scheduler, bundle, resources);
  }
}
