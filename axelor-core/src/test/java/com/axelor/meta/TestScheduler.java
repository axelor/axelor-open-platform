/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.axelor.AbstractTest;
import com.axelor.JpaTestModule;
import com.axelor.meta.db.MetaSchedule;
import com.axelor.meta.db.repo.MetaScheduleRepository;
import com.axelor.quartz.JobRunner;
import com.axelor.quartz.SchedulerModule;
import com.axelor.test.GuiceModules;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

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

    @Inject private Scheduler scheduler;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
      assertNotNull(scheduler);
      jobCounter += 1;
    }
  }

  @Inject private JobRunner runner;

  @Inject private MetaScheduleRepository schedules;

  private static int jobCounter = 0;

  @BeforeEach
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
    int current = 0;
    jobCounter = 0;
    runner.init();
    Thread.sleep(3000);
    current = jobCounter;
    assertEquals(4, current);

    jobCounter = 0;
    runner.restart();
    Thread.sleep(5000);
    current = jobCounter;
    assertEquals(6, current);

    runner.shutdown();
  }
}
