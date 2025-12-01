/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.inject.Beans;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Background job that processes pending audit logs asynchronously. Converts raw AuditLog records
 * into MailMessage notifications.
 */
public class AuditProcessorJob implements Job {

  private static final Logger log = LoggerFactory.getLogger(AuditProcessorJob.class);

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    var processor = Beans.get(AuditProcessor.class);
    try {
      processor.processAll();
    } catch (Exception e) {
      log.error("Error processing audit logs", e);
      throw new JobExecutionException(e);
    }
  }
}
