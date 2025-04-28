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

import static org.quartz.impl.StdSchedulerFactory.AUTO_GENERATE_INSTANCE_ID;
import static org.quartz.impl.StdSchedulerFactory.PROP_CONNECTION_PROVIDER_CLASS;
import static org.quartz.impl.StdSchedulerFactory.PROP_DATASOURCE_PREFIX;
import static org.quartz.impl.StdSchedulerFactory.PROP_JOB_STORE_CLASS;
import static org.quartz.impl.StdSchedulerFactory.PROP_JOB_STORE_PREFIX;
import static org.quartz.impl.StdSchedulerFactory.PROP_SCHED_INSTANCE_ID;
import static org.quartz.impl.StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME;
import static org.quartz.impl.StdSchedulerFactory.PROP_THREAD_POOL_PREFIX;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.cache.CacheConfig;
import com.axelor.cache.CacheProviderInfo;
import com.axelor.cache.CacheType;
import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.axelor.db.internal.DBHelper;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.jdbcjobstore.HSQLDBDelegate;
import org.quartz.impl.jdbcjobstore.JobStoreSupport;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.impl.jdbcjobstore.PostgreSQLDelegate;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.impl.jdbcjobstore.oracle.OracleDelegate;
import org.quartz.spi.JobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Provider} for {@link Scheduler} that uses {@link GuiceJobFactory} so that services can
 * be injected to the job instances.
 */
@Singleton
class SchedulerProvider implements Provider<Scheduler> {

  private static final String INSTANCE_NAME = "AxelorScheduler";
  private static final String DEFAULT_THREAD_COUNT = "3";

  @Inject private GuiceJobFactory jobFactory;

  @Inject private JobCleaner jobCleaner;

  private static final Logger log = LoggerFactory.getLogger(SchedulerProvider.class);

  @Override
  public Scheduler get() {

    AppSettings settings = AppSettings.get();
    Properties cfg = new Properties();

    // Thread pool configuration
    cfg.put(
        key(PROP_THREAD_POOL_PREFIX, "threadCount"),
        settings.get(AvailableAppSettings.QUARTZ_THREAD_COUNT, DEFAULT_THREAD_COUNT));

    String jobStoreClassName = settings.get(AvailableAppSettings.QUARTZ_JOB_STORE_CLASS);
    boolean isClustered =
        CacheConfig.getAppCacheProvider()
            .flatMap(CacheProviderInfo::getCacheType)
            .map(CacheType::isDistributed)
            .orElse(false);

    if (isClustered) {
      if (StringUtils.isBlank(jobStoreClassName)) {
        jobStoreClassName = JobStoreTX.class.getName();
      }
      cfg.put(key(PROP_JOB_STORE_PREFIX, "isClustered"), "true");
      log.info("Quartz clustering enabled");
    }

    if (StringUtils.notBlank(jobStoreClassName)) {
      Class<? extends JobStore> jobStoreClass;
      try {
        jobStoreClass = Class.forName(jobStoreClassName).asSubclass(JobStore.class);
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(e);
      }

      if (JobStoreSupport.class.isAssignableFrom(jobStoreClass)) {
        configureJDBCJobStore(cfg, jobStoreClassName);
      }
    }

    Scheduler scheduler;
    SchedulerFactory schedulerFactory;
    try {
      schedulerFactory = new GuiceSchedulerFactory(cfg);
      scheduler = schedulerFactory.getScheduler();
      scheduler.setJobFactory(jobFactory);
      scheduler.getListenerManager().addJobListener(jobCleaner);
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }

    return scheduler;
  }

  /** Configures JDBC-based JobStore. */
  private void configureJDBCJobStore(Properties cfg, String jobStoreClassName) {
    AppSettings settings = AppSettings.get();

    // Main scheduler properties
    cfg.put(PROP_SCHED_INSTANCE_NAME, INSTANCE_NAME);
    cfg.put(PROP_SCHED_INSTANCE_ID, AUTO_GENERATE_INSTANCE_ID);

    String dataSourceName =
        Optional.ofNullable(DBHelper.getDataSourceName())
            .filter(StringUtils::notBlank)
            .orElse("default");

    // Job store configuration
    cfg.put(PROP_JOB_STORE_CLASS, jobStoreClassName);
    cfg.put(key(PROP_JOB_STORE_PREFIX, "driverDelegateClass"), getDriverDelegateClass());
    cfg.put(key(PROP_JOB_STORE_PREFIX, "dataSource"), dataSourceName);

    // {@link AxelorConnectionProvider} already defaults auto-commit to false
    cfg.put(key(PROP_JOB_STORE_PREFIX, "dontSetAutoCommitFalse"), "true");

    // DataSource configuration
    cfg.put(
        key(PROP_DATASOURCE_PREFIX, dataSourceName, PROP_CONNECTION_PROVIDER_CLASS),
        AxelorConnectionProvider.class.getName());

    Inflector inflector = Inflector.getInstance();

    // Properties to set on the job store directly via reflection.
    Map<String, String> jobStoreProps =
        settings
            .getPropertiesStartingWith(AvailableAppSettings.QUARTZ_JOB_STORE_PREFIX)
            .entrySet()
            .stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    entry ->
                        key(
                            PROP_JOB_STORE_PREFIX,
                            inflector.camelize(
                                entry
                                    .getKey()
                                    .substring(
                                        AvailableAppSettings.QUARTZ_JOB_STORE_PREFIX.length()),
                                true)),
                    Map.Entry::getValue));
    cfg.putAll(jobStoreProps);

    // Properties to set on the data source directly via reflection.
    // Exclude the keys that are handled by {@link AxelorConnectionProvider}
    Map<String, String> dataSourceProps =
        settings
            .getPropertiesStartingWith(AvailableAppSettings.QUARTZ_DATA_SOURCE_PREFIX)
            .entrySet()
            .stream()
            .map(
                entry ->
                    Map.entry(
                        inflector.camelize(
                            entry
                                .getKey()
                                .substring(AvailableAppSettings.QUARTZ_DATA_SOURCE_PREFIX.length()),
                            true),
                        entry.getValue()))
            .collect(
                Collectors.toUnmodifiableMap(
                    entry -> key(PROP_DATASOURCE_PREFIX, dataSourceName, entry.getKey()),
                    Map.Entry::getValue));

    cfg.putAll(dataSourceProps);
  }

  /**
   * Determines the appropriate Quartz JDBC driver delegate class.
   *
   * @return fully qualified class name of the Quartz DriverDelegate
   */
  private String getDriverDelegateClass() {
    if (DBHelper.isPostgreSQL()) {
      return PostgreSQLDelegate.class.getName();
    } else if (DBHelper.isOracle()) {
      return OracleDelegate.class.getName();
    } else if (DBHelper.isHSQL()) {
      return HSQLDBDelegate.class.getName();
    }

    // Fallback to standard JDBC driver (suitable for MySQL, H2)
    return StdJDBCDelegate.class.getName();
  }

  private String key(String... key) {
    return Arrays.stream(key).collect(Collectors.joining("."));
  }
}
