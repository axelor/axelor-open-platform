/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.quartz;

import static org.quartz.utils.Key.DEFAULT_GROUP;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.internal.DBHelper;
import com.axelor.db.tenants.TenantAware;
import com.axelor.db.tenants.TenantConfig;
import com.axelor.db.tenants.TenantConfigProvider;
import com.axelor.db.tenants.TenantModule;
import com.axelor.db.tenants.TenantResolver;
import com.axelor.event.Observes;
import com.axelor.events.PreRequest;
import com.axelor.events.qualifiers.EntityType;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.CallMethod;
import com.axelor.meta.db.MetaSchedule;
import com.axelor.meta.db.MetaScheduleParam;
import com.axelor.meta.db.repo.MetaScheduleRepository;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link JobRunner} controls the scheduler.<br>
 * <br>
 * It configures the {@link Scheduler} from the job configuration provided from the database. It
 * also provides some public methods to start/restart/stop the scheduler.
 */
@Singleton
public class JobRunner {

  private static Logger log = LoggerFactory.getLogger(JobRunner.class);
  private static final String META_SCHEDULE_QUERY =
      "SELECT DISTINCT self FROM MetaSchedule self LEFT JOIN FETCH self.params";

  private Scheduler scheduler;

  @CallMethod
  public boolean isEnabled() {
    return AppSettings.get().getBoolean(AvailableAppSettings.QUARTZ_ENABLE, false);
  }

  private boolean isStopped() {
    try {
      return scheduler == null || scheduler.isShutdown();
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  /** Configure all schedulers. */
  private void configure() {
    log.info("Configuring scheduled jobs...");

    if (TenantModule.isEnabled()) {
      findGroups().forEach(this::configure);
    } else {
      configure(TenantResolver.currentTenantIdentifier(), findSchedules());
    }
  }

  private void configure(String group, List<MetaSchedule> schedules) {
    if (schedules != null) {
      schedules.forEach(schedule -> this.configure(group, schedule));
      long count = schedules.stream().filter(x -> Boolean.TRUE.equals(x.getActive())).count();
      log.info("Configured total jobs: {}, group: {}", count, group);
    }
  }

  /**
   * Configure the given scheduler
   *
   * @param group the tenant
   * @param schedule the schedule
   */
  private void configure(String group, MetaSchedule schedule) {

    if (schedule == null || !Boolean.TRUE.equals(schedule.getActive())) {
      return;
    }

    // use tenant id as job group
    final String name = schedule.getName();
    final String cron = schedule.getCron();
    final String jobClass = schedule.getJob();

    log.info("Configuring job: {}, group: {}, cron: {}", name, group, cron);
    Class<?> klass;
    try {
      klass = Class.forName(jobClass);
    } catch (ClassNotFoundException e1) {
      log.error("No such job class found: {}", jobClass);
      return;
    }
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
    if (schedule.getParams() != null) {
      for (MetaScheduleParam param : schedule.getParams()) {
        data.put(param.getName(), param.getValue());
      }
    }

    @SuppressWarnings("unchecked")
    final JobDetail detail =
        JobBuilder.newJob((Class<? extends Job>) klass)
            .withIdentity(name, group)
            .withDescription(schedule.getDescription())
            .usingJobData(data)
            .build();

    final Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(name, group)
            .withDescription(schedule.getDescription())
            .withSchedule(cronSchedule)
            .build();

    try {
      scheduler.scheduleJob(detail, trigger);
    } catch (ObjectAlreadyExistsException e) {
      // This is expected if job is already persisted
      // or in a clustered environment if another node already scheduled it.
      // Catching ObjectAlreadyExistsException is preferred over
      // checkExists() followed by scheduleJob() which would not be atomic.
      log.info("Job already exists: {}", name);
    } catch (SchedulerException e) {
      log.error("Unable to configure scheduled job: {}", name, e);
    }
  }

  /**
   * Update the given job.
   *
   * @param schedule the scheduled job
   * @throws SchedulerException
   */
  public void update(MetaSchedule schedule) throws SchedulerException {
    String group = TenantResolver.currentTenantIdentifier();
    update(group, schedule);
  }

  /**
   * Update the given job for the given tenant group.
   *
   * @param group tenant group
   * @param schedule the scheduled job
   * @throws SchedulerException
   */
  private void update(String group, MetaSchedule schedule) throws SchedulerException {
    Objects.requireNonNull(schedule);
    Preconditions.checkState(isEnabled(), I18n.get("The scheduler service is disabled."));
    Preconditions.checkState(!isStopped(), I18n.get("The scheduler service has been stopped."));

    this.remove(group, schedule);

    if (Boolean.TRUE.equals(schedule.getActive())) {
      configure(group, schedule);
    }
  }

  /**
   * Update all the jobs for the given tenant group.
   *
   * @param group the tenant group
   * @throws SchedulerException
   */
  public void update(String group) throws SchedulerException {
    List<MetaSchedule> schedules = findSchedules(group);
    for (MetaSchedule schedule : schedules) {
      update(group, schedule);
    }
  }

  /**
   * Remove the given job.
   *
   * @param schedule the job to remove
   */
  public void remove(MetaSchedule schedule) throws SchedulerException {
    String group = TenantResolver.currentTenantIdentifier();
    remove(group, schedule);
  }

  /**
   * Remove the given job.
   *
   * @param group the tenant group
   * @param schedule the job to remove
   */
  private void remove(String group, MetaSchedule schedule) throws SchedulerException {
    Preconditions.checkNotNull(schedule);
    String name = schedule.getName();
    JobKey jobKey = new JobKey(name, group);
    if (scheduler.checkExists(jobKey)) {
      log.info("Deleting job: {}, group {}", name, group);
      scheduler.deleteJob(jobKey);
    }
  }

  /**
   * Remove all the jobs of the given tenant group.
   *
   * @param group the tenant group
   */
  public void remove(String group) throws SchedulerException {
    log.info("Deleting jobs from group {}", group);
    scheduler.deleteJobs(new ArrayList<>(scheduler.getJobKeys(GroupMatcher.groupEquals(group))));
  }

  /** Initialize the scheduler. */
  public void init() {
    if (!isEnabled()) {
      throw new IllegalStateException(I18n.get("The scheduler service is disabled."));
    }
    if (isStopped()) {
      scheduler = Beans.get(Scheduler.class);
    }
    this.configure();
    log.info("Starting scheduler...");
    try {
      scheduler.start();
    } catch (SchedulerException e) {
      log.error("Unable to start the scheduler...");
      log.trace("Scheduler error: {}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
    log.info("Job scheduler is running...");
  }

  /** Shutdown the scheduler. */
  public void shutdown() {
    if (isStopped()) {
      log.info("The job scheduler is already stopped.");
      return;
    }

    log.info("Stopping scheduler...");

    try {
      scheduler.shutdown(true);
      scheduler = null;
    } catch (SchedulerException e) {
      log.error("Unable to stop the scheduler...");
      log.trace("Scheduler error: {}", e.getMessage(), e);
    }
    log.info("The job scheduler stopped.");
  }

  /** Restart tasks */
  public void restart() {
    String group = TenantResolver.currentTenantIdentifier();
    if (group == null) {
      group = DEFAULT_GROUP;
    }
    try {
      this.remove(group);
    } catch (SchedulerException e) {
      log.error("Unable to clear jobs...");
      log.trace("Scheduler error: {}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
    this.configure();
  }

  /** Stop tasks */
  public void stop() {
    String group = TenantResolver.currentTenantIdentifier();
    if (group == null) {
      group = DEFAULT_GROUP;
    }
    try {
      scheduler.pauseJobs(GroupMatcher.groupEquals(group));
    } catch (SchedulerException e) {
      log.error("Unable to pause jobs...");
      log.trace("Scheduler error: {}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  public void onRemove(
      @Observes @Named(PreRequest.REMOVE) @EntityType(MetaSchedule.class) PreRequest event) {

    if (!isEnabled() || isStopped()) return;

    List<Object> records = new ArrayList<>();
    if (event.getRequest().getRecords().isEmpty()) {
      records.add(event.getRequest().getData());
    } else {
      records.addAll(event.getRequest().getRecords());
    }

    for (Object item : records) {
      Long id =
          item instanceof MetaSchedule metaSchedule
              ? metaSchedule.getId()
              : Longs.tryParse(((Map<?, ?>) item).get("id").toString());
      MetaSchedule record = Beans.get(MetaScheduleRepository.class).find(id);
      try {
        this.remove(record);
      } catch (SchedulerException e) {
        // ignore
      }
    }
  }

  private List<MetaSchedule> findSchedules() {
    return JPA.em().createQuery(META_SCHEDULE_QUERY, MetaSchedule.class).getResultList();
  }

  private List<MetaSchedule> findSchedules(String group) {
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final AtomicReference<List<MetaSchedule>> result = new AtomicReference<>();
    try {
      executor
          .submit(
              new TenantAware(
                      () -> {
                        result.set(
                            JPA.em()
                                .createQuery(META_SCHEDULE_QUERY, MetaSchedule.class)
                                .getResultList());
                      })
                  .tenantId(group))
          .get();
    } catch (InterruptedException | ExecutionException e) {
      log.warn("Unable to find jobs, group: {}", group);
    } finally {
      executor.shutdown();
    }
    return result.get();
  }

  private Map<String, List<MetaSchedule>> findGroups() {
    final Map<String, List<MetaSchedule>> groups = new HashMap<>();
    final List<TenantConfig> all = Beans.get(TenantConfigProvider.class).findAll();
    final ForkJoinPool pool = new ForkJoinPool(DBHelper.getMaxWorkers());
    try {
      final List<Future<?>> futures =
          all.stream()
              .filter(x -> Boolean.TRUE.equals(x.getActive()))
              .map(TenantConfig::getTenantId)
              .filter(StringUtils::notBlank)
              .map(group -> pool.submit(() -> groups.put(group, this.findSchedules(group))))
              .collect(Collectors.toList());
      for (Future<?> future : futures) {
        future.get();
      }
    } catch (InterruptedException | ExecutionException e) {
      log.warn("Unable to find jobs...");
    } finally {
      pool.shutdown();
    }
    return groups;
  }
}
