/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.meta.loader;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.db.Model;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

abstract class AbstractLoader {
	
	protected Logger log = LoggerFactory.getLogger(getClass().getSuperclass());

	private final ThreadLocal<Set<String>> visited = new ThreadLocal<>();
	private final ThreadLocal<Map<Class<?>, Multimap<String, Object>>> unresolved = new ThreadLocal<>();

	protected final void clear() {
		visited.remove();
		unresolved.remove();
	}

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
	 * Check whether the entity is updated.<br>
	 * <br>
	 * The entity is updated if it's not null and version value is greater then
	 * 0 (zero).
	 * 
	 * @param entity
	 *            the entity instance
	 * @return true if updated false otherwise
	 */
	protected boolean isUpdated(Model entity) {
		if (entity == null || entity.getVersion() == null) {
			return false;
		}
		return entity.getVersion() > 0;
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

	public final void load(Module module, boolean update) {
		try {
			doLoad(module, update);
		} finally {
			clear();
		}
	}
}
