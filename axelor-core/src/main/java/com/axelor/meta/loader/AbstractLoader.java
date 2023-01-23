/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.meta.loader;

import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractLoader {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractLoader.class);

  private static final Map<Entry<Class<?>, String>, Boolean> visited = new ConcurrentHashMap<>();
  private static final Set<String> duplicates = ConcurrentHashMap.newKeySet();
  private static final Map<Entry<Class<?>, String>, Set<Long>> unresolved =
      new ConcurrentHashMap<>();
  private static final Collection<Runnable> resolveTasks = new ConcurrentLinkedQueue<>();

  /**
   * Checks whether an element is already visited. Element can be either identified the pair {@code
   * type}/{@code name} or by its {@code xmlId}.
   *
   * @param type element type
   * @param name element name
   * @param baseType element base type
   * @param xmlId element xmlId
   * @return whether the element is already visited or not
   */
  protected boolean isVisited(Class<?> type, String name, Class<?> baseType, String xmlId) {
    final Class<?> entryType;
    final String entryName;
    final boolean withoutId = StringUtils.isBlank(xmlId);

    if (withoutId) {
      entryType = type;
      entryName = name;
    } else {
      entryType = baseType;
      entryName = xmlId;
    }

    if (visited.putIfAbsent(Map.entry(entryType, entryName), Boolean.TRUE) == null) {
      return false;
    }

    duplicates.add(entryName);

    LOG.error(
        "Duplicate {} found {} 'id': {}",
        type.getSimpleName(),
        withoutId ? "without" : "with",
        entryName);
    return true;
  }

  /**
   * Checks whether an element is already visited. Element can be either identified the pair {@code
   * type}/{@code name} or by its {@code xmlId}.
   *
   * @param type element type
   * @param name element name
   * @param xmlId element xmlId
   * @return whether the element is already visited or not
   */
  protected boolean isVisited(Class<?> type, String name, String xmlId) {
    return isVisited(type, name, type, xmlId);
  }

  /**
   * Returns items that have been visited several times.
   *
   * @return duplicate items
   */
  protected Set<String> getDuplicates() {
    return duplicates;
  }

  /**
   * Put a value of the given type for resolution for the given unresolved key.<br>
   * <br>
   * The value is put inside a {@link Map} with unresolvedKey as the key.
   *
   * @param type
   * @param unresolvedKey
   * @param entityId
   */
  protected <T> void setUnresolved(Class<T> type, String unresolvedKey, Long entityId) {
    final Set<Long> entityIds =
        unresolved.computeIfAbsent(
            Map.entry(type, unresolvedKey), key -> ConcurrentHashMap.newKeySet());
    entityIds.add(entityId);
  }

  /**
   * Resolve the given unresolved key.<br>
   * <br>
   * All the pending values of the unresolved key are returned for further processing. The values
   * are removed from the backing {@link Map}.
   *
   * @param type the type of pending objects
   * @param unresolvedKey the unresolved key
   * @return a set of all the pending objects
   */
  protected <T> Set<Long> resolve(Class<T> type, String unresolvedKey) {
    final Set<Long> entityIds = unresolved.remove(Map.entry(type, unresolvedKey));
    if (entityIds == null) {
      return Collections.emptySet();
    }
    return entityIds;
  }

  protected void addResolveTask(
      Class<?> type, String name, Long entityId, BiConsumer<Long, Long> consumer) {
    final Runnable task = () -> resolve(type, name).forEach(id -> consumer.accept(id, entityId));
    resolveTasks.add(task);
  }

  protected void runResolveTasks() {
    synchronized (resolveTasks) {
      if (resolveTasks.isEmpty()) {
        return;
      }

      resolveTasks.forEach(task -> JPA.runInTransaction(task::run));
      resolveTasks.clear();
    }
  }

  /**
   * Return set of all the unresolved keys.
   *
   * @return set of unresolved keys
   */
  protected Set<String> unresolvedKeys() {
    return unresolved.keySet().stream().map(Entry::getValue).collect(Collectors.toSet());
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
    visited.clear();
    duplicates.clear();
    unresolved.clear();
    resolveTasks.clear();
  }

  public final void load(Module module, boolean update) {
    doLoad(module, update);
  }
}
