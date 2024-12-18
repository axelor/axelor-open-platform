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
package org.apache.shiro.cache.jcache;

import jakarta.inject.Inject;
import java.util.Optional;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import org.apache.shiro.cache.Cache;

/** Shiro JCache Manager where we can pass configuration to created caches. */
public class AxelorJCacheManager extends JCacheManager {

  private final Configuration<?, ?> config;

  @Inject
  public AxelorJCacheManager(
      Configuration<String, Object> config, Optional<CacheManager> optionalCacheManager) {
    this.config = config;
    optionalCacheManager.ifPresent(this::setCacheManager);
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
}
