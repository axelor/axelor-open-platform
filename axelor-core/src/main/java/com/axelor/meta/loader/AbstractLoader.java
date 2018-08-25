/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.meta.loader;

import com.axelor.db.JPA;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractLoader {

  protected Logger log = LoggerFactory.getLogger(getClass().getSuperclass());

  private static final Set<Entry<Class<?>, String>> visited = new HashSet<>();
  private static final Map<Class<?>, Multimap<String, Long>> unresolved = new HashMap<>();
  private static final List<Runnable> resolveTasks = new ArrayList<>();

  /**
   * Check whether the given name is already visited.
   *
   * @param type the key type
   * @param name the key name
   * @return true if the name is already visited false otherwise
   */
  protected boolean isVisited(Class<?> type, String name) {
    synchronized (visited) {
      Entry<Class<?>, String> key = new SimpleImmutableEntry<>(type, name);
      if (visited.contains(key)) {
        log.error("duplicate {} found: {}", type.getSimpleName(), name);
        return true;
      }
      visited.add(key);
      return false;
    }
  }

  /**
   * Put a value of the given type for resolution for the given unresolved key.<br>
   * <br>
   * The value is put inside a {@link Multimap} with unresolvedKey as the key.
   *
   * @param type
   * @param unresolvedKey
   * @param entityId
   */
  protected <T> void setUnresolved(Class<T> type, String unresolvedKey, Long entityId) {
    synchronized (unresolved) {
      final Multimap<String, Long> mm =
          unresolved.computeIfAbsent(type, key -> HashMultimap.create());
      mm.put(unresolvedKey, entityId);
    }
  }

  /**
   * Resolve the given unresolved key.<br>
   * <br>
   * All the pending values of the unresolved key are returned for further processing. The values
   * are removed from the backing {@link Multimap}.
   *
   * @param type the type of pending objects
   * @param unresolvedKey the unresolved key
   * @return a set of all the pending objects
   */
  protected <T> Set<Long> resolve(Class<T> type, String unresolvedKey) {
    synchronized (unresolved) {
      Set<Long> entityIds = Sets.newHashSet();
      Multimap<String, Long> mm = unresolved.get(type);
      if (mm == null) {
        return entityIds;
      }
      for (Long item : mm.get(unresolvedKey)) {
        entityIds.add((Long) item);
      }
      mm.removeAll(unresolvedKey);
      return entityIds;
    }
  }

  protected void addResolveTask(
      Class<?> type, String name, Long entityId, BiConsumer<Long, Long> consumer) {
    Runnable task = () -> resolve(type, name).forEach(id -> consumer.accept(id, entityId));
    synchronized (resolveTasks) {
      resolveTasks.add(task);
    }
  }

  protected void runResolveTasks() {
    if (resolveTasks.isEmpty()) {
      return;
    }

    synchronized (resolveTasks) {
      resolveTasks.parallelStream().forEach(task -> JPA.runInTransaction(task::run));
      resolveTasks.clear();
    }
  }

  /**
   * Return set of all the unresolved keys.
   *
   * @return set of unresolved keys
   */
  protected Set<String> unresolvedKeys() {
    synchronized (unresolved) {
      Set<String> names = Sets.newHashSet();
      for (Multimap<String, Long> mm : unresolved.values()) {
        names.addAll(mm.keySet());
      }
      return names;
    }
  }

  /**
   * Implement this method the load the data.
   *
   * @param module the module for which to load the data
   * @param update whether to force update while loading
   */
  protected abstract void doLoad(Module module, boolean update);

  /**
   * This method is called by the module installer as last step when loading of all modules is
   * complete.
   *
   * @param module the module the process
   * @param update whether to update
   */
  void doLast(Module module, boolean update) {}

  static void doCleanUp() {
    synchronized (visited) {
      visited.clear();
    }
    synchronized (unresolved) {
      unresolved.clear();
    }
    synchronized (resolveTasks) {
      resolveTasks.clear();
    }
  }

  public final void load(Module module, boolean update) {
    doLoad(module, update);
  }
}
