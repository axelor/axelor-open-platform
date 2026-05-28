/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.apache.shiro.cache.jcache;

import com.axelor.app.AvailableAppSettings;
import com.axelor.db.tenants.TenantModule;
import com.axelor.event.Observes;
import com.axelor.events.ShutdownEvent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;

/** Shiro JCache Manager where we can pass configuration to created caches. */
public class AxelorJCacheManager extends JCacheManager {

  private final Configuration<?, ?> config;
  private final long sessionTimeoutMinutes;

  private static final String PREFIX = "shiro:";

  @Inject
  public AxelorJCacheManager(
      @Named("shiro") Configuration<Object, Object> config,
      @Named("shiro") CacheManager cacheManager,
      @Named(AvailableAppSettings.SESSION_TIMEOUT) long sessionTimeoutMinutes) {
    this.config = config;
    this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    setCacheManager(cacheManager);
  }

  @Override
  public <K, V> Cache<K, V> getCache(String name) throws CacheException {
    javax.cache.Cache<K, V> cache =
        needsTenantAware(name)
            ? new TenantAwareJCache<>(
                tenant -> getJCache("%s%s:%s".formatted(PREFIX, tenant, name)),
                sessionTimeoutMinutes)
            : getJCache(PREFIX + name);

    // Access to package private JCacheManager#JCache
    return new JCacheManager.JCache<>(cache);
  }

  private <K, V> javax.cache.Cache<K, V> getJCache(String name) {
    var manager = getCacheManager();
    javax.cache.Cache<K, V> cache = manager.getCache(name);

    if (cache == null) {
      synchronized (this) {
        cache = manager.getCache(name);
        if (cache == null) {
          @SuppressWarnings("unchecked")
          var typedConfig = (Configuration<K, V>) config;
          cache = manager.createCache(name, typedConfig);
        }
      }
    }

    return cache;
  }

  private boolean needsTenantAware(String name) {
    // Active session cache uses unique session IDs and does not need to be tenant-aware.
    // Other caches may rely on subject principals that are database-dependent.
    return TenantModule.isEnabled() && !CachingSessionDAO.ACTIVE_SESSION_CACHE_NAME.equals(name);
  }

  public <K, V> Configuration<K, V> getConfig() {
    @SuppressWarnings("unchecked")
    var typedConfig = (Configuration<K, V>) config;
    return typedConfig;
  }

  protected void onAppShutdownEvent(@Observes ShutdownEvent event) {
    getCacheManager().close();
  }
}
