/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.quartz;

import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.RequestScoper;
import com.google.inject.servlet.ServletScopes;
import java.util.Collections;
import org.eclipse.core.runtime.jobs.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.core.JobRunShell;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.spi.TriggerFiredBundle;

/** Custom {@link JobRunShell} to ensure {@link Job} can use {@link RequestScoped} services. */
public class GuiceJobRunShell extends JobRunShell {

  private QuartzScheduler sched;
  private QuartzSchedulerResources resources;

  public GuiceJobRunShell(
      Scheduler scheduler, TriggerFiredBundle bndle, QuartzSchedulerResources resources) {
    super(scheduler, bndle);
    this.resources = resources;
  }

  @Override
  public void initialize(QuartzScheduler sched) throws SchedulerException {
    // we'll initialize in run method
    this.sched = sched;
  }

  @Override
  public void run() {
    final RequestScoper scope = ServletScopes.scopeRequest(Collections.emptyMap());
    try (RequestScoper.CloseableScope ignored = scope.open()) {
      try {
        super.initialize(this.sched);
        super.run();
      } catch (SchedulerException e) {
        resources
            .getJobStore()
            .triggeredJobComplete(
                this.firedTriggerBundle.getTrigger(),
                this.firedTriggerBundle.getJobDetail(),
                CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
      }
    }
  }
}
