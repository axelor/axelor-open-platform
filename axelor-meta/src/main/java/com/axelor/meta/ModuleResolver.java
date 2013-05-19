package com.axelor.meta;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Module dependency resolver.
 * 
 */
public final class ModuleResolver {

	private HashMap<String, Module> modules = Maps.newHashMap();

	private class Module {

		private String name;
		
		private List<Module> depends = Lists.newArrayList();

		public Module(String name) {
			this.name = name;
		}

		public void dependsOn(Module module) {
			if (!depends.contains(module)) {
				depends.add(module);
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (obj == null) return false;
			if (!(obj instanceof Module)) return false;
			return name.equals(((Module) obj).name);
		}
		
		@Override
		public String toString() {
			 return pprint(1);
		}

		private String pprint(int depth) {
			StringBuilder builder = new StringBuilder();
			builder.append(name).append("\n");
			for(Module dep : depends) {
				builder.append(Strings.repeat("    ", depth))
					   .append("-> ")
					   .append(dep.pprint(depth+1));
			}
			return builder.toString();
		}
	}

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
		for(Module dep : module.depends) {
			if (!resolved.contains(dep.name)) {
				if (unresolved.contains(dep.name)) {
					throw new IllegalArgumentException("Circular dependency detected: " + name + " -> " + dep.name);
				}
				resolve(dep.name, resolved, unresolved);
			}
		}
		resolved.add(name);
		unresolved.remove(name);
	}

	/**
	 * Add a module name to resolve.
	 * 
	 * @param name a module name
	 */
	public void add(String name) {
		module(name);
	}

	/**
	 * Add a module with dependency information.
	 * 
	 * @param name a module name
	 * @param depends name of modules it depends on
	 */
	public void add(String name, String... depends) {
		Module module = module(name);
		for(String dep : depends) {
			module.dependsOn(module(dep));
		}
	}

	/**
	 * Resolve a module dependency.
	 * 
	 * @param name a module name
	 * @return list of the resolved dependencies.
	 */
	public List<String> resolve(String name) {
		List<String> resolved = Lists.newArrayList();
		Set<String> unresolved = Sets.newHashSet();

		this.resolve(name, resolved, unresolved);
		
		if (!unresolved.isEmpty()) {
			throw new IllegalArgumentException("Unresolved dependencies: " + unresolved);
		}
		
		return resolved;
	}

	/**
	 * Returns all the resolved modules.
	 * 
	 */
	public List<String> all() {
		
		List<String> resolved = Lists.newArrayList();
		List<List<String>> resolutions = Lists.newArrayList();

		for(String key : modules.keySet()) {
			resolutions.add(resolve(key));
		}

		Collections.sort(resolutions, new Comparator<List<?>>() {
			@Override
			public int compare(List<?> o1, List<?> o2) {
				return Integer.compare(o1.size(), o2.size());
			}
		});

		for(List<String> resolution : resolutions) {
			for(String name : resolution) {
				if (resolved.contains(name)) continue;
				resolved.add(name);
			}
		}
		
		return resolved;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for(Module m : modules.values()) {
			builder.append(m).append("\n\n");
		}
		return builder.toString();
	}
}