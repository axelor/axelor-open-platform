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

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import com.google.inject.Injector;

/**
 * The custom {@link JobFactory} to create job instance using guice injector.
 * 
 */
@Singleton
class GuiceJobFactory implements JobFactory {

	private Injector injector;

	@Inject
	public GuiceJobFactory(Injector injector) {
		this.injector = injector;
	}

	@Override
	public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
		Class<? extends Job> jobClass = bundle.getJobDetail().getJobClass();
		return this.injector.getInstance(jobClass);
	}
}