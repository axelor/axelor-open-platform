package com.axelor.db;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.hibernate.ejb.packaging.NativeScanner;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
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
			Reflections reflections = new Reflections(
					new ConfigurationBuilder()
					.addUrls(ClasspathHelper.forPackage("com.axelor"))
					.setScanners(new SubTypesScanner()));
			List<String> names = Lists.newArrayList();
			for (Class<?> klass : reflections.getSubTypesOf(Model.class)) {
				if (Model.class.isAssignableFrom(klass) && (
						klass.isAnnotationPresent(Entity.class) ||
						klass.isAnnotationPresent(Embeddable.class) ||
						klass.isAnnotationPresent(MappedSuperclass.class))) {
					if (cache.containsKey(klass.getName())) {
						continue;
					}
					cache.put(klass.getName(), klass);
					nameCache.put(klass.getSimpleName(), klass.getName());
					names.add(klass.toString());
				}
			}
			Collections.sort(names);
			log.info("Model classes found:\n  " + Joiner.on("\n  ").join(names));
		}
		return new HashSet<Class<?>>(cache.values());
	}
	
	public static Class<?> findModel(String name) {
		if (cache == null) {
			findModels();
		}
		String className = nameCache.containsKey(name) ? nameCache.get(name) : name;
		return cache.get(className);
	}
}
