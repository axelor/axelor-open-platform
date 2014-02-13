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

	private static int jobCounter = 0;
	
	@Before
	@Transactional
	public void setUp() {
		if (MetaSchedule.all().count() > 0) {
			return;
		}
		
		final MetaSchedule meta = new MetaSchedule("my.job");
		meta.setDescription("My Job");
		meta.setCron("0/1 * * * * ?");
		meta.setJob(MyJob.class.getName());
		meta.setActive(true);
		meta.save();
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
