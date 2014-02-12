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

	public static ClassLoader loader = new JpaClassLoader();

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
			
			final Set<Class<? extends Model>> models = Reflections.findSubTypesOf(Model.class)
					.within("com.axelor")
					.having(Entity.class)
					.having(Embeddable.class)
					.having(MappedSuperclass.class)
					.any().find();
			for (Class<?> klass : models) {
				if (cache.containsKey(klass.getName())) {
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
