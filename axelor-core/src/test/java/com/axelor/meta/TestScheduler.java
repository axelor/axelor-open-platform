/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.meta;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import com.axelor.AbstractTest;
import com.axelor.JpaTestModule;
import com.axelor.meta.db.MetaSchedule;
import com.axelor.meta.db.repo.MetaScheduleRepository;
import com.axelor.quartz.JobRunner;
import com.axelor.quartz.SchedulerModule;
import com.axelor.test.GuiceModules;
import com.google.inject.persist.Transactional;

@GuiceModules(TestScheduler.MyModule.class)
public class TestScheduler extends AbstractTest {
	
	public static class MyModule extends JpaTestModule {

		@Override
		protected void configure() {
			super.configure();
			install(new SchedulerModule());
		}
	}

	public static class MyJob implements Job {
		
		@Inject
		private Scheduler scheduler;
		
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			Assert.assertNotNull(scheduler);
			jobCounter += 1;
		}
	}
	
	@Inject
	private JobRunner runner;
	
	@Inject
	private MetaScheduleRepository schedules;

	private static int jobCounter = 0;
	
	@Before
	@Transactional
	public void setUp() {
		if (schedules.all().count() > 0) {
			return;
		}
		
		final MetaSchedule meta = new MetaSchedule("my.job");
		meta.setDescription("My Job");
		meta.setCron("0/1 * * * * ?");
		meta.setJob(MyJob.class.getName());
		meta.setActive(true);
		schedules.save(meta);
	}
	
	@Test
	public void test() throws SchedulerException, InterruptedException {
		jobCounter = 0;
		runner.start();
		Thread.sleep(3000);
		Assert.assertEquals(4, jobCounter);
		
		jobCounter = 0;
		runner.restart();
		Thread.sleep(5000);
		Assert.assertEquals(6, jobCounter);
		
		runner.stop();
	}
}
