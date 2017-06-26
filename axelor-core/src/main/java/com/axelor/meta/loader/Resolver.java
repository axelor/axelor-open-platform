/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.axelor.common.StringUtils;

/**
 * Module dependency resolver.
 * 
 */
final class Resolver {

	private HashMap<String, Module> modules = new LinkedHashMap<>();

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
		Stream.of(depends)
			.filter(StringUtils::notBlank)
			.map(this::module)
			.forEach(m -> module.dependsOn(m));
		return module;
	}

	/**
	 * Resolve a module dependency.
	 * 
	 * @param name a module name
	 * @return list of the resolved dependencies.
	 */
	public List<Module> resolve(String name) {
		final List<String> resolved = new ArrayList<>();
		final Set<String> unresolved = new HashSet<>();

		this.resolve(name, resolved, unresolved);

		if (!unresolved.isEmpty()) {
			throw new IllegalArgumentException("Unresolved dependencies: " + unresolved);
		}

		return resolved.stream().map(this::module).collect(Collectors.toList());
	}

	/**
	 * Returns all the resolved modules.
	 * 
	 * @return list of module in dependency order.
	 */
	public List<Module> all() {
		final List<List<Module>> resolutions = new ArrayList<>();
		final List<Module> resolved = new ArrayList<>();

		for (String key : modules.keySet()) {
			resolutions.add(resolve(key));
		}

		for (List<Module> resolution : resolutions) {
			for (Module module : resolution) {
				if (!resolved.contains(module)) {
					resolved.add(module);
				}
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
		return all().stream().map(Module::getName).collect(Collectors.toList());
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