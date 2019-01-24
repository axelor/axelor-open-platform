/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import com.axelor.app.AppSettings;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;

/**
 * The {@link Provider} for {@link Scheduler} that uses {@link GuiceJobFactory} so that services can
 * be injected to the job instances.
 */
@Singleton
class SchedulerProvider implements Provider<Scheduler> {

  private static final String DEFAULT_THREAD_COUNT = "3";

  private static final String THREAD_COUNT_GET = "quartz.threadCount";
  private static final String THREAD_COUNT_SET = "org.quartz.threadPool.threadCount";

  @Inject private GuiceJobFactory jobFactory;

  @Override
  public Scheduler get() {

    Properties cfg = new Properties();
    cfg.put(THREAD_COUNT_SET, AppSettings.get().get(THREAD_COUNT_GET, DEFAULT_THREAD_COUNT));

    Scheduler scheduler;
    SchedulerFactory schedulerFactory;
    try {
      schedulerFactory = new GuiceSchedulerFactory(cfg);
      scheduler = schedulerFactory.getScheduler();
      scheduler.setJobFactory(jobFactory);
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }

    return scheduler;
  }
}
