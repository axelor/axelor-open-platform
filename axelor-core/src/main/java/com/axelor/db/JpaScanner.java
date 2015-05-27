/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
	
	private static ConcurrentMap<String, Class<?>> modelCache = null;
	private static ConcurrentMap<String, Class<?>> repoCache = null;

	private static ConcurrentMap<String, String> modelNames = new MapMaker().makeMap();
	private static ConcurrentMap<String, String> repoNames = new MapMaker().makeMap();

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

		if (modelCache != null) {
			return new HashSet<Class<?>>(modelCache.values());
		}

		modelCache = new MapMaker().makeMap();
		repoCache = new MapMaker().makeMap();

		synchronized (modelCache) {
			
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
				if (modelCache.containsKey(klass.getName()) ||
					excludes.contains(klass.getPackage().getName())) {
					continue;
				}
				register(klass);
			}
			log.info("Total found: {}", modelCache.size());
		}

		synchronized (repoCache) {
			log.info("Searching for repository classes...");
			ClassFinder<?> finder = Reflections.findSubTypesOf(JpaRepository.class)
					.within("com.axelor");

			for (String pkg : includes) {
				finder = finder.within(pkg);
			}
			for (Class<?> klass : finder.any().find()) {
				if (repoCache.containsKey(klass.getName()) ||
					excludes.contains(klass.getPackage().getName())) {
					continue;
				}
				repoCache.put(klass.getName(), klass);
				repoNames.put(klass.getSimpleName(), klass.getName());
			}
			log.info("Total found: {}", repoCache.size());
		}
		return new HashSet<Class<?>>(modelCache.values());
	}

	private static void register(Class<?> model) {
		modelCache.put(model.getName(), model);
		modelNames.put(model.getSimpleName(), model.getName());
	}

	public static ClassLoader getClassLoader() {
		return loader;
	}

	public static Class<?> findModel(String name) {
		if (modelCache == null) {
			findModels();
		}
		String className = modelNames.containsKey(name) ? modelNames.get(name) : name;
		return modelCache.get(className);
	}

	public static Class<?> findRepository(String name) {
		if (modelCache == null) {
			findModels();
		}
		String className = repoNames.containsKey(name) ? repoNames.get(name) : name;
		return repoCache.get(className);
	}
}
