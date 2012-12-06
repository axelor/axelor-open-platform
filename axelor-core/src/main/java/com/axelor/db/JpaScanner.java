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
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapMaker;

/**
 * A custom Hibernate scanner that scans all the classpath entries for all the
 * {@link Model} classes annotated with {@link Entity}.
 * 
 */
public class JpaScanner extends NativeScanner {

	private static Logger LOG = LoggerFactory.getLogger(JpaScanner.class);
	
	private static ConcurrentMap<String, Class<?>> CACHE = null;
	
	@Override
	public Set<Class<?>> getClassesInJar(URL jarToScan, Set<Class<? extends Annotation>> annotationsToLookFor) {
		
		Set<Class<?>> mine = super.getClassesInJar(jarToScan, annotationsToLookFor);
		for (Class<?> klass : mine) {
			if (!Model.class.isAssignableFrom(klass)) {
				LOG.warn("Not a Model: " + klass.getName());
				return mine;
			}
		}
		return findModels();
	}
	
	public static Set<Class<?>> findModels() {
		
		if (CACHE != null) {
			return new HashSet<Class<?>>(CACHE.values());
		}
		
		CACHE = new MapMaker().makeMap();
		synchronized (CACHE) {
			Reflections reflections = new Reflections(
					new ConfigurationBuilder()
					.addUrls(ClasspathHelper.forPackage("com.axelor"))
					.setScanners(new SubTypesScanner()));
			for (Class<?> klass : reflections.getSubTypesOf(Model.class)) {
				if (Model.class.isAssignableFrom(klass) && (
						klass.isAnnotationPresent(Entity.class) ||
						klass.isAnnotationPresent(Embeddable.class) ||
						klass.isAnnotationPresent(MappedSuperclass.class))) {
					if (CACHE.containsKey(klass.getName()))
						continue;
					CACHE.put(klass.getName(), klass);
					LOG.debug("Found Model: " + klass.getName());
				}
			}
		}
		return new HashSet<Class<?>>(CACHE.values());
	}
}
