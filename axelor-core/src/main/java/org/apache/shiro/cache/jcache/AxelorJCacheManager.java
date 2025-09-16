/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.apache.shiro.cache.jcache;

import com.axelor.event.Observes;
import com.axelor.events.ShutdownEvent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import org.apache.shiro.cache.Cache;

/** Shiro JCache Manager where we can pass configuration to created caches. */
public class AxelorJCacheManager extends JCacheManager {

  private final Configuration<?, ?> config;

  @Inject
  public AxelorJCacheManager(
      @Named("shiro") Configuration<Object, Object> config,
      @Named("shiro") CacheManager cacheManager) {
    this.config = config;
    setCacheManager(cacheManager);
  }

  @Override
  public <K, V> Cache<K, V> getCache(String name) throws CacheException {
    var manager = getCacheManager();
    var prefixedName = "shiro:" + name;
    javax.cache.Cache<K, V> cache = manager.getCache(prefixedName);

    if (cache == null) {
      synchronized (this) {
        cache = manager.getCache(prefixedName);
        if (cache == null) {
          @SuppressWarnings("unchecked")
          var typedConfig = (Configuration<K, V>) config;
          cache = manager.createCache(prefixedName, typedConfig);
        }
      }
    }

    // Access to package private JCacheManager#JCache
    return new JCacheManager.JCache<>(cache);
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
