/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
package com.axelor.db;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.hibernate.ejb.packaging.NativeScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.common.reflections.ClassFinder;
import com.axelor.common.reflections.Reflections;
import com.google.common.collect.MapMaker;

/**
 * A custom Hibernate scanner that scans all the classpath entries for all the
 * {@link Model} classes annotated with {@link Entity}.
 *
 */
public class JpaScanner extends NativeScanner {

	private static Logger log = LoggerFactory.getLogger(JpaScanner.class);
	
	private static ConcurrentMap<String, Class<?>> cache = null;

	private static ConcurrentMap<String, String> nameCache = new MapMaker().makeMap();

	private static Set<String> excludes = new HashSet<>();
	private static Set<String> includes = new HashSet<>();

	public static ClassLoader loader = new JpaClassLoader();

	/**
	 * Exclude classes from the given package.
	 *
	 * @param pkg the package name
	 */
	public static void exclude(String pkg) {
		includes.remove(pkg);
		excludes.add(pkg);
	}

	/**
	 * Include classes from the given package.
	 *
	 * @param pkg the package name
	 */
	public static void include(String pkg) {
		excludes.remove(pkg);
		includes.add(pkg);
	}

	@Override
	public Set<Class<?>> getClassesInJar(URL jarToScan, Set<Class<? extends Annotation>> annotationsToLookFor) {
		Set<Class<?>> mine = super.getClassesInJar(jarToScan, annotationsToLookFor);
		for (Class<?> klass : mine) {
			if (!Model.class.isAssignableFrom(klass)) {
				log.warn("Not a Model: " + klass.getName());
				return mine;
			}
		}
		return findModels();
	}

	public static Set<Class<?>> findModels() {

		if (cache != null) {
			return new HashSet<Class<?>>(cache.values());
		}

		cache = new MapMaker().makeMap();
		synchronized (cache) {
			
			log.info("Searching for model classes...");
			
			register(Model.class);

			ClassFinder<Model> finder = Reflections.findSubTypesOf(Model.class)
					.within("com.axelor")
					.having(Entity.class)
					.having(Embeddable.class)
					.having(MappedSuperclass.class);

			for (String pkg : includes) {
				finder = finder.within(pkg);
			}

			final Set<Class<? extends Model>> models = finder.any().find();

			for (Class<?> klass : models) {
				if (cache.containsKey(klass.getName()) ||
					excludes.contains(klass.getPackage().getName())) {
					continue;
				}
				register(klass);
			}
			log.info("Total found: {}", cache.size());
		}
		return new HashSet<Class<?>>(cache.values());
	}

	private static void register(Class<?> model) {
		cache.put(model.getName(), model);
		nameCache.put(model.getSimpleName(), model.getName());
	}

	public static ClassLoader getClassLoader() {
		return loader;
	}

	public static Class<?> findModel(String name) {
		if (cache == null) {
			findModels();
		}
		String className = nameCache.containsKey(name) ? nameCache.get(name) : name;
		return cache.get(className);
	}
}
