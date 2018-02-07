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
package com.axelor.meta.loader;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

abstract class AbstractLoader {
	
	protected Logger log = LoggerFactory.getLogger(getClass().getSuperclass());

	private static final ThreadLocal<Set<String>> visited = new ThreadLocal<>();
	private static final ThreadLocal<Map<Class<?>, Multimap<String, Object>>> unresolved = new ThreadLocal<>();

	/**
	 * Check whether the given name is already visited.
	 * 
	 * @param type
	 *            the key type
	 * @param name
	 *            the key name
	 * @return true if the name is already visited false otherwise
	 */
	protected boolean isVisited(Class<?> type, String name) {
		if (visited.get() == null) {
			visited.set(Sets.<String>newHashSet());
		}
		if (visited.get().contains(type + name)) {
			log.error("duplicate found: {}", name);
			return true;
		}
		visited.get().add(type + name);
		return false;
	}

	/**
	 * Put a value of the given type for resolution for the given unresolved
	 * key.<br>
	 * <br>
	 * The value is put inside a {@link Multimap} with unresolvedKey as the key.
	 * 
	 * @param type
	 * @param unresolvedKey
	 * @param value
	 */
	protected <T> void setUnresolved(Class<T> type, String unresolvedKey, T value) {
		Map<Class<?>, Multimap<String, Object>> map = unresolved.get();
		if (map == null) {
			map = Maps.newHashMap();
			unresolved.set(map);
		}
		Multimap<String, Object> mm = map.get(type);
		if (mm == null) {
			mm = HashMultimap.create();
			map.put(value.getClass(), mm);
		}
		mm.put(unresolvedKey, value);
	}
	
	/**
	 * Resolve the given unresolved key.<br>
	 * <br>
	 * All the pending values of the unresolved key are returned for further
	 * processing. The values are removed from the backing {@link Multimap}.
	 * 
	 * @param type
	 *            the type of pending objects
	 * @param unresolvedKey
	 *            the unresolved key
	 * @return a set of all the pending objects
	 */
	@SuppressWarnings("unchecked")
	protected <T> Set<T> resolve(Class<T> type, String unresolvedKey) {
		Set<T> values = Sets.newHashSet();
		Map<Class<?>, Multimap<String, Object>> map = unresolved.get();
		if (map == null) {
			return values;
		}
		Multimap<String, Object> mm = map.get(type);
		if (mm == null) {
			return values;
		}
		for (Object item : mm.get(unresolvedKey)) {
			values.add((T) item);
		}
		mm.removeAll(unresolvedKey);
		return values;
	}
	
	/**
	 * Return set of all the unresolved keys.
	 * 
	 * @return set of unresolved keys
	 */
	protected Set<String> unresolvedKeys() {
		Set<String> names = Sets.newHashSet();
		Map<Class<?>, Multimap<String, Object>> map = unresolved.get();
		if (map == null) {
			return names;
		}
		for (Multimap<String, Object> mm : map.values()) {
			names.addAll(mm.keySet());
		}
		return names;
	}

	/**
	 * Implement this method the load the data.
	 * 
	 * @param module
	 *            the module for which to load the data
	 * @param update
	 *            whether to force update while loading
	 */
	protected abstract void doLoad(Module module, boolean update);

	/**
	 * This method is called by the module installer as last step when loading
	 * of all modules is complete.
	 * 
	 * @param module
	 *            the module the process
	 * @param update
	 *            whether to update
	 */
	void doLast(Module module, boolean update) {

	}

	static void doCleanUp() {
		visited.remove();
		unresolved.remove();
	}

	public final void load(Module module, boolean update) {
		doLoad(module, update);
	}
}
