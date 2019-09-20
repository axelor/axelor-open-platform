/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
