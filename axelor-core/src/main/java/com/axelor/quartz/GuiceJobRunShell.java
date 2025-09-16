/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.quartz;

import static org.quartz.utils.Key.DEFAULT_GROUP;

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
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.core.JobRunShell;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.utils.Key;
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
        .map(JobDetail::getKey)
        .map(Key::getGroup) // group is tenant id
        .filter(group -> StringUtils.notBlank(group) && !DEFAULT_GROUP.equals(group))
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
    if (config != null && Boolean.TRUE.equals(config.getActive())) {
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
