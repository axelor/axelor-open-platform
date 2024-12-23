/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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

import com.axelor.common.StringUtils;
import com.axelor.db.tenants.TenantAware;
import com.axelor.db.tenants.TenantConfig;
import com.axelor.db.tenants.TenantConfigProvider;
import com.axelor.inject.Beans;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.RequestScoper;
import com.google.inject.servlet.ServletScopes;
import java.util.Collections;
import java.util.Optional;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.core.JobRunShell;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Custom {@link JobRunShell} to ensure {@link Job} can use {@link RequestScoped} services. */
public class GuiceJobRunShell extends JobRunShell {

  private static final Logger LOG = LoggerFactory.getLogger(GuiceJobRunShell.class);

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

  private void doRun() {
    Optional.ofNullable(firedTriggerBundle.getJobDetail())
        .map(x -> x.getKey())
        .map(x -> x.getGroup()) // group is tenant id
        .filter(StringUtils::notBlank)
        .ifPresentOrElse(this::run, super::run);
  }

  private void superRun() {
    final RequestScoper scope = ServletScopes.scopeRequest(Collections.emptyMap());
    try (RequestScoper.CloseableScope ignored = scope.open()) {
      super.run();
    }
  }

  private void run(String tenantId) {
    final TenantConfig config = Beans.get(TenantConfigProvider.class).find(tenantId);
    if (Boolean.TRUE.equals(config.getActive())) {
      // JobRunShell may re-use same thread from the pool, so run the task in a new
      // thread to ensure the task is always run with a proper tenant id.
      TenantAware task = new TenantAware(this::superRun).tenantId(tenantId).withTransaction(false);
      task.start();
      try {
        task.join();
      } catch (InterruptedException e) {
        handleError(e);
      }
    }
  }

  private void handleError(Throwable e) {
    LOG.error("Job failed with error!", e);
    resources
        .getJobStore()
        .triggeredJobComplete(
            this.firedTriggerBundle.getTrigger(),
            this.firedTriggerBundle.getJobDetail(),
            CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
  }

  @Override
  public void run() {
    try {
      super.initialize(this.sched);
      this.doRun();
    } catch (SchedulerException e) {
      handleError(e);
    }
  }
}
