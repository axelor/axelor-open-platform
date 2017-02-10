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
package com.axelor.db;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.scan.internal.ClassDescriptorImpl;
import org.hibernate.boot.archive.scan.internal.ScanResultImpl;
import org.hibernate.boot.archive.scan.spi.AbstractScannerImpl;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor.Categorization;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.common.reflections.ClassFinder;
import com.axelor.meta.MetaScanner;
import com.google.common.collect.MapMaker;

/**
 * A custom Hibernate scanner that scans all the classpath entries for all the
 * {@link Model} classes annotated with {@link Entity}.
 *
 */
public class JpaScanner extends AbstractScannerImpl {

	private static Logger log = LoggerFactory.getLogger(JpaScanner.class);
	
	private static ConcurrentMap<String, Class<?>> modelCache = null;
	private static ConcurrentMap<String, Class<?>> repoCache = null;

	private static ConcurrentMap<String, String> modelNames = new MapMaker().makeMap();
	private static ConcurrentMap<String, String> repoNames = new MapMaker().makeMap();

	private static Set<String> excludes = new HashSet<>();
	private static Set<String> includes = new HashSet<>();

	public static ClassLoader loader = new JpaClassLoader();

	public JpaScanner() {
		super(StandardArchiveDescriptorFactory.INSTANCE);
	}

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
	public ScanResult scan(ScanEnvironment environment, ScanOptions options, ScanParameters params) {
		final ScanResult found = super.scan(environment, options, params);
		final Set<Class<?>> models = findModels();
		final Set<ClassDescriptor> descriptors = new HashSet<>();

		if (found.getLocatedClasses() != null) {
			descriptors.addAll(found.getLocatedClasses());
		}

		for (Class<?> model : models) {
			ClassDescriptor descriptor = new ClassDescriptorImpl(model.getName(), Categorization.MODEL, null);
			descriptors.add(descriptor);
		}

		return new ScanResultImpl(found.getLocatedPackages(), descriptors, found.getLocatedMappingFiles());
	}

	public static Set<Class<?>> findModels() {

		if (modelCache != null) {
			return new HashSet<Class<?>>(modelCache.values());
		}

		modelCache = new MapMaker().makeMap();
		repoCache = new MapMaker().makeMap();

		synchronized (modelCache) {
			
			log.debug("Searching for entity classes...");
			
			register(Model.class);

			ClassFinder<Model> finder = MetaScanner.findSubTypesOf(Model.class)
					.having(Entity.class)
					.having(Embeddable.class)
					.having(MappedSuperclass.class);

			for (String pkg : includes) {
				finder = finder.within(pkg);
			}

			for (Class<?> klass : finder.any().find()) {
				if (modelCache.containsKey(klass.getName()) ||
					excludes.contains(klass.getPackage().getName())) {
					continue;
				}
				log.trace("Found entity: {}", klass.getName());
				register(klass);
			}
			log.debug("Entitity classes found: {}", modelCache.size());
		}

		synchronized (repoCache) {
			log.debug("Searching for repository classes...");

			ClassFinder<?> finder = MetaScanner.findSubTypesOf(JpaRepository.class);

			for (String pkg : includes) {
				finder = finder.within(pkg);
			}

			for (Class<?> klass : finder.any().find()) {
				if (repoCache.containsKey(klass.getName()) ||
					excludes.contains(klass.getPackage().getName())) {
					continue;
				}
				log.trace("Found repository: {}", klass.getName());
				repoCache.put(klass.getName(), klass);
				repoNames.put(klass.getSimpleName(), klass.getName());
			}
			log.debug("Repository classes found: {}", repoCache.size());
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
