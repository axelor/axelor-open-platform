/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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

import com.axelor.app.AppSettings;
import com.axelor.common.ClassUtils;
import com.axelor.db.Query;
import com.axelor.i18n.I18n;
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
	private static final String CONFIG_QUARTZ_ENABLE = "quartz.enable";

	private Scheduler scheduler;
	
	private int total;
	
	@Inject
	public JobRunner(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
	
	public boolean isEnabled() {
		return AppSettings.get().getBoolean(CONFIG_QUARTZ_ENABLE, false);
	}

	private void configure() {
		if (total > 0) {
			return;
		}
		total = 0;
		log.info("Configuring scheduled jobs...");
		for (MetaSchedule meta : Query.of(MetaSchedule.class).fetch()) {
			configure(meta);
		}
		log.info("Configured total jobs: {}", total);
	}
	
	private void configure(MetaSchedule meta) {

		if (meta == null || meta.getActive() != Boolean.TRUE) {
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
			throw new IllegalArgumentException(I18n.get("Invalid cron: ") + cron);
		}
	}

	/**
	 * Start the scheduler.
	 * 
	 */
	public void start() {
		if (!isEnabled()) {
			throw new IllegalStateException(I18n.get("The scheduler service is disabled."));
		}
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
