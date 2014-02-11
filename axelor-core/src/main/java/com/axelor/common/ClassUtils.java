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
package com.axelor.common;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import com.google.common.collect.Sets;

/**
 * The class provides some static helper methods to deal with classes.
 * 
 */
public final class ClassUtils {
	
	/**
	 * The helper class to find subtypes of a given super class.
	 * 
	 */
	public static class ClassFinder<T> {

		private Class<T> type;
		
		private ConfigurationBuilder builder = new ConfigurationBuilder();
		private Set<Class<? extends Annotation>> annotations = Sets.newLinkedHashSet();
		
		private boolean matchAll = true;
		private boolean hasUrls = false;
		
		private ClassFinder(Class<T> type) {
			this.type = type;
			this.builder.addUrls(ClasspathHelper.forClassLoader());
			this.builder.addScanners(new SubTypesScanner());	
		}
		
		/**
		 * Only search within the given package name.
		 * 
		 * @param packageName
		 *            the package name
		 * @return same class finder instance
		 */
		public ClassFinder<T> within(String packageName) {
			if (!hasUrls) {
				hasUrls = true;
				builder.getUrls().clear();
			}
			builder.addUrls(ClasspathHelper.forPackage(packageName));
			return this;
		}
		
		/**
		 * Only search classes with the given annotation.
		 * 
		 * @param annotation
		 *            the annotation to check
		 * @return same class finder instance
		 */
		public ClassFinder<T> having(final Class<? extends Annotation> annotation) {
			this.annotations.add(annotation);
			return this;
		}
		
		/**
		 * In case of multiple {@link #having(Class)} calls, whether to check
		 * any one annotation (by default all annotations are checked).
		 * 
		 * @return same class finder instance
		 */
		public ClassFinder<T> any() {
			this.matchAll = false;
			return this;
		}
		
		private boolean hasAnnotation(Class<?> cls) {
			boolean matched = false;
			for (Class<? extends Annotation> annotation : annotations) {
				if (cls.isAnnotationPresent(annotation)) {
					if (!matchAll) {
						return true;
					}
					matched = true;
				} else if (matchAll) {
					return false;
				}
			}
			return annotations.size() == 0 || matched;
		}

		/**
		 * Find the classes.
		 * 
		 * @return set of matched classes
		 */
		public Set<Class<? extends T>> find() {
			final Reflections reflections = new Reflections(builder);
			final Set<Class<? extends T>> all = Sets.newLinkedHashSet();
			for (Class<? extends T> cls : reflections.getSubTypesOf(type)) {
				if (hasAnnotation(cls)) {
					all.add(cls);
				}
			}
			return all;
		}
	}
	
	/**
	 * Return a class finder to find all the subtypes of the given type.
	 * 
	 * @param type
	 *            the super type
	 * @return class finder
	 */
	public static <T> ClassFinder<T> finderOf(Class<T> type) {
		return new ClassFinder<T>(type);
	}
	
	/**
	 * This is same as {@link Class#forName(String)} but throws
	 * {@link IllegalArgumentException} if class is not found.
	 * 
	 * @param name
	 *            name of the class to look for
	 * @return the class found
	 */
	public static Class<?> findClass(String name) {
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(name, e);
		}
	}

	/**
	 * Finds the resource with the given name.
	 * 
	 * @param name
	 *            The resource name
	 * @return resource an {@link URL} for reading the resource or null
	 * @see ClassLoader#getResource(String)
	 */
	public static URL getResource(String name) {
		return Thread.currentThread().getContextClassLoader().getResource(name);
	}

	/**
	 * Returns an input stream for reading the specified resource.
	 * 
	 * @param name
	 *            The resource name
	 * @return An input stream for reading the resource or null
	 * @see ClassLoader#getResourceAsStream(String)
	 */
	public static InputStream getResourceStream(String name) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
	}
}
