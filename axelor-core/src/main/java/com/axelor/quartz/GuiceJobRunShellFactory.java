/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
