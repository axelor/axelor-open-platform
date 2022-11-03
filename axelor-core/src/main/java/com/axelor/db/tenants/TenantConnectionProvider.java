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
package com.axelor.db.tenants;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import java.util.Objects;
import javax.sql.DataSource;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The tenant connection provider. */
public class TenantConnectionProvider
    extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String>
    implements ServiceRegistryAwareService, Stoppable {

  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LoggerFactory.getLogger(TenantConnectionProvider.class);

  private transient TenantConfigProvider configProvider;

  private final transient LoadingCache<String, HikariDataSource> dataSourceCache =
      Caffeine.newBuilder()
          .weakValues()
          .expireAfterAccess(Duration.ofHours(1))
          .removalListener(
              (String id, HikariDataSource source, RemovalCause cause) -> {
                if (source != null) {
                  source.close();
                }
              })
          .build(
              tenantIdentifier ->
                  createDataSource(validate(configProvider.find(tenantIdentifier))));

  @Override
  protected final DataSource selectAnyDataSource() {
    return selectDataSource(TenantConfig.DEFAULT_TENANT_ID);
  }

  @Override
  protected final DataSource selectDataSource(String tenantIdentifier) {
    if (configProvider.find(tenantIdentifier) == null) {
      dataSourceCache.invalidate(tenantIdentifier);
      LOGGER.debug("no such tenant found: {}", tenantIdentifier);
      throw new TenantNotFoundException(tenantIdentifier);
    }
    LOGGER.debug("using tenant: {}", tenantIdentifier);
    return dataSourceCache.get(tenantIdentifier);
  }

  private HikariDataSource createDataSource(TenantConfig config) {
    LOGGER.debug("creating datasource for tenant config: {}", config);

    final AppSettings settings = AppSettings.get();
    final HikariConfig hc = new HikariConfig();

    hc.setDataSourceJNDI(config.getJndiDataSource());
    hc.setDriverClassName(config.getJdbcDriver());
    hc.setJdbcUrl(config.getJdbcUrl());
    hc.setUsername(config.getJdbcUser());
    hc.setPassword(config.getJdbcPassword());
    hc.setAutoCommit(false);

    hc.setIdleTimeout(settings.getInt(AvailableAppSettings.HIBERNATE_HIKARI_IDLE_TIMEOUT, 300000));
    hc.setMaximumPoolSize(
        settings.getInt(AvailableAppSettings.HIBERNATE_HIKARI_MAXIMUM_POOL_SIZE, 20));
    hc.setMinimumIdle(0);

    return new HikariDataSource(hc);
  }

  private TenantConfig validate(TenantConfig config) {
    Objects.requireNonNull(config, "invalid tenant config.");
    if (config.getJndiDataSource() != null) {
      return config;
    }
    Preconditions.checkState(config.getJdbcDriver() != null, "no jdbc driver specified.");
    Preconditions.checkState(config.getJdbcUrl() != null, "no jdbc url specified.");
    return config;
  }

  @Override
  public void stop() {
    dataSourceCache.invalidateAll();
  }

  @Override
  public void injectServices(ServiceRegistryImplementor serviceRegistry) {
    configProvider = TenantSupport.get().getConfigProvider();
    LOGGER.debug("using tenant config provider: {}", configProvider.getClass().getName());
  }
}
