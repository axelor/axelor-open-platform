/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail.service;

import com.axelor.inject.Beans;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

/** Job to fetch emails. */
public class MailFetchJob implements Job {

  private boolean isRunning(JobExecutionContext context) {
    try {
      return context.getScheduler().getCurrentlyExecutingJobs().stream()
          .filter(j -> j.getTrigger().equals(context.getTrigger()))
          .filter(j -> !j.getFireInstanceId().equals(context.getFireInstanceId()))
          .findFirst()
          .isPresent();
    } catch (SchedulerException e) {
      return false;
    }
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    if (isRunning(context)) {
      return;
    }
    final MailService service = Beans.get(MailService.class);
    try {
      service.fetch();
    } catch (Exception e) {
      throw new JobExecutionException(e);
    }
  }
}
