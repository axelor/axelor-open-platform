/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.quartz;

import java.util.Properties;
import org.quartz.Scheduler;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.core.JobRunShellFactory;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.impl.StdSchedulerFactory;

/** Custom {@link StdSchedulerFactory} to use {@link GuiceJobRunShellFactory}. */
public class GuiceSchedulerFactory extends StdSchedulerFactory {

  public GuiceSchedulerFactory(Properties props) throws SchedulerException {
    super(props);
  }

  @Override
  protected Scheduler instantiate(QuartzSchedulerResources rsrcs, QuartzScheduler qs) {
    Scheduler scheduler = super.instantiate(rsrcs, qs);
    JobRunShellFactory jrsf = new GuiceJobRunShellFactory(rsrcs);
    rsrcs.setJobRunShellFactory(jrsf);
    try {
      jrsf.initialize(scheduler);
    } catch (SchedulerConfigException e) {
      // this should not happen
    }
    return scheduler;
  }
}
