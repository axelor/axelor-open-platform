/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.quartz;

import com.axelor.inject.Beans;
import jakarta.inject.Singleton;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/** The custom {@link JobFactory} to create job injectable job instances. */
@Singleton
class GuiceJobFactory implements JobFactory {

  @Override
  public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
    Class<? extends Job> jobClass = bundle.getJobDetail().getJobClass();
    return Beans.get(jobClass);
  }
}
