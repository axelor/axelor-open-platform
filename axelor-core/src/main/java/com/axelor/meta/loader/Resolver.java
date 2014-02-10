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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.axelor.common.StringUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Module dependency resolver.
 * 
 */
final class Resolver {

	private HashMap<String, Module> modules = Maps.newHashMap();

	private Module module(String name) {
		Module module = modules.get(name);
		if (module == null) {
			module = new Module(name);
			modules.put(name, module);
		}
		return module;
	}

	private void resolve(String name, List<String> resolved, Set<String> unresolved) {
		Module module = modules.get(name);
		unresolved.add(name);
		if (module == null) {
			return;
		}
		for(Module dep : module.getDepends()) {
			if (!resolved.contains(dep.getName())) {
				if (unresolved.contains(dep.getName())) {
					throw new IllegalArgumentException("Circular dependency detected: " + name + " -> " + dep.getName());
				}
				resolve(dep.getName(), resolved, unresolved);
			}
		}
		resolved.add(name);
		unresolved.remove(name);
	}

	/**
	 * Add a module with dependency information.
	 * 
	 * @param name a module name
	 * @param depends name of modules it depends on
	 * @return a {@link Module} instance
	 */
	public Module add(String name, String... depends) {
		Module module = module(name);
		for(String dep : depends) {
			if (!StringUtils.isBlank(dep)) {
				module.dependsOn(module(dep));
			}
		}
		return module;
	}

	/**
	 * Resolve a module dependency.
	 * 
	 * @param name a module name
	 * @return list of the resolved dependencies.
	 */
	public List<Module> resolve(String name) {
		
		List<String> resolved = Lists.newArrayList();
		Set<String> unresolved = Sets.newHashSet();

		this.resolve(name, resolved, unresolved);
		
		if (!unresolved.isEmpty()) {
			throw new IllegalArgumentException("Unresolved dependencies: " + unresolved);
		}
		
		List<Module> all = Lists.newArrayList();
		for (String n : resolved) {
			all.add(module(n));
		}
		
		return all;
	}

	/**
	 * Returns all the resolved modules.
	 * 
	 * @return list of module in dependency order.
	 */
	public List<Module> all() {
		
		List<Module> resolved = Lists.newArrayList();
		List<List<Module>> resolutions = Lists.newArrayList();

		for(String key : modules.keySet()) {
			resolutions.add(resolve(key));
		}

		Collections.sort(resolutions, new Comparator<List<?>>() {
			@Override
			public int compare(List<?> o1, List<?> o2) {
				return Integer.compare(o1.size(), o2.size());
			}
		});

		for(List<Module> resolution : resolutions) {
			for(Module module : resolution) {
				if (resolved.contains(module)) continue;
				resolved.add(module);
			}
		}
		
		return resolved;
	}
	
	/**
	 * Return resolved module names.
	 * 
	 * @return list of module names in dependency order.
	 */
	public List<String> names() {
		List<Module> all = this.all();
		List<String> names = Lists.newArrayList();
		for (Module module : all) {
			names.add(module.getName());
		}
		return names;
	}
	
	/**
	 * Get the instance of the module by given name.
	 * 
	 * @param name
	 *            the module name.
	 * @return the {@link Module} instance or null if no module by that name
	 */
	public Module get(String name) {
		return modules.get(name);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for(Module m : modules.values()) {
			builder.append(m.pprint(1)).append("\n\n");
		}
		return builder.toString();
	}
}