/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.quartz;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.common.ClassUtils;
import com.axelor.meta.db.MetaSchedule;
import com.axelor.meta.db.MetaScheduleParam;
import com.google.common.base.Throwables;

/**
 * The {@link JobRunner} controls the scheduler.<br>
 * <br>
 * It configures the {@link Scheduler} from the job configuration provided from
 * the database. It also provides some public methods to start/restart/stop the
 * scheduler.
 * 
 */
@Singleton
public class JobRunner {

	private static Logger log = LoggerFactory.getLogger(JobRunner.class);

	private Scheduler scheduler;
	
	private int total;

	@Inject
	public JobRunner(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	private void configure() {
		if (total > 0) {
			return;
		}
		total = 0;
		log.info("Configuring scheduled jobs...");
		for (MetaSchedule meta : MetaSchedule.all().fetch()) {
			configure(meta);
		}
		log.info("Configured total jobs: {}", total);
	}
	
	private void configure(MetaSchedule meta) {

		if (meta == null || meta.getActive() == Boolean.FALSE) {
			return;
		}
		
		final String name = meta.getName();
		final String cron = meta.getCron();
		final String jobClass = meta.getJob();

		log.info("Configuring job: {}, {}", name, cron);
		final Class<?> klass = ClassUtils.findClass(jobClass);
		if (klass == null || !Job.class.isAssignableFrom(klass)) {
			log.error("Invalid job class: {}", jobClass);
			return;
		}

		final CronScheduleBuilder cronSchedule;
		try {
			cronSchedule = CronScheduleBuilder.cronSchedule(cron);
		} catch (Exception e) {
			log.error("Invalid cron string: {}", cron);
			return;
		}
		
		final JobDataMap data = new JobDataMap();
		if (meta.getParams() != null) {
			for (MetaScheduleParam param : meta.getParams()) {
				data.put(param.getName(), param.getValue());
			}
		}

		@SuppressWarnings("unchecked")
		final JobDetail detail = JobBuilder
			.newJob((Class<? extends Job>) klass)
			.withIdentity(name)
			.withDescription(meta.getDescription())
			.usingJobData(data)
			.build();

		final Trigger trigger = TriggerBuilder
				.newTrigger()
				.withIdentity(name)
				.withDescription(meta.getDescription())
				.withSchedule(cronSchedule)
				.build();

		try {
			scheduler.scheduleJob(detail, trigger);
		} catch (SchedulerException e) {
			log.error("Unable to configure scheduled job: {}", name, e);
		}

		total += 1;
	}

	/**
	 * Validate the given cron string.
	 * 
	 * @param cron the cron string to validate
	 */
	public void validate(String cron) {
		try {
			CronScheduleBuilder.cronSchedule(cron);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid cron: " + cron);
		}
	}

	/**
	 * Start the scheduler.
	 * 
	 */
	public void start() {
		this.configure();
		log.info("Starting scheduler...");
		try {
			scheduler.start();
		} catch (SchedulerException e) {
			log.error("Unable to start the scheduler...");
			log.trace("Scheduler error: {}", e.getMessage(), e);
			throw Throwables.propagate(e);
		}
		log.info("Job scheduler is running...");
	}

	/**
	 * Stop the scheduler.
	 * 
	 */
	public void stop() {
		log.info("Stoping scheduler...");
		try {
			scheduler.shutdown();
		} catch (SchedulerException e) {
			log.error("Unable to stop the scheduler...");
			log.trace("Scheduler error: {}", e.getMessage(), e);
		}
		log.info("The job scheduler stopped.");
	}

	/**
	 * Reconfigure the scheduler and restart.
	 * 
	 */
	public void restart() {
		try {
			scheduler.clear();
		} catch (SchedulerException e) {
			log.error("Unable to clear existing jobs...");
			log.trace("Scheduler error: {}", e.getMessage(), e);
			throw Throwables.propagate(e);
		}
		total = 0;
		this.start();
	}
}
