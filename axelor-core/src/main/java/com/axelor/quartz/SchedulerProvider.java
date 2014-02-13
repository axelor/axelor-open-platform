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

import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

import com.axelor.app.AppSettings;
import com.google.common.base.Throwables;

/**
 * The {@link Provider} for {@link Scheduler} that uses {@link GuiceJobFactory}
 * so that services can be injected to the job instances.
 * 
 */
@Singleton
class SchedulerProvider implements Provider<Scheduler> {

	private static final String DEFAULT_THREAD_COUNT = "3";
	
	private static final String THREAD_COUNT_GET = "quartz.threadCount";
	private static final String THREAD_COUNT_SET = "org.quartz.threadPool.threadCount";

	@Inject
	private GuiceJobFactory jobFactory;
	
	@Override
	public Scheduler get() {

		Properties cfg = new Properties();
		cfg.put(THREAD_COUNT_SET, AppSettings.get().get(THREAD_COUNT_GET, DEFAULT_THREAD_COUNT));
		
		Scheduler scheduler;
		SchedulerFactory schedulerFactory;
		try {
			schedulerFactory = new StdSchedulerFactory(cfg);
			scheduler = schedulerFactory.getScheduler();
			scheduler.setJobFactory(jobFactory);
		} catch (SchedulerException e) {
			throw Throwables.propagate(e);
		}

		return scheduler;
	}
}