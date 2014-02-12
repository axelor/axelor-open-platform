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
package com.axelor.common.reflections;

import java.lang.annotation.Annotation;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * The helper class to find sub types of a given super class.
 * 
 */
public final class ClassFinder<T> {

	private Class<T> type;
	private Set<Class<? extends Annotation>> annotations = Sets.newLinkedHashSet();
	private Set<String> packages = Sets.newLinkedHashSet();
	private ClassLoader loader = Thread.currentThread().getContextClassLoader();
	
	private boolean matchAll = true;
	
	ClassFinder(Class<T> type) {
		this.type = type;
	}
	
	/**
	 * Only search within the given package name.
	 * 
	 * @param packageName
	 *            the package name
	 * @return same class finder instance
	 */
	public ClassFinder<T> within(String packageName) {
		packages.add(packageName);
		return this;
	}

	/**
	 * Search using the given class loader.
	 * 
	 * @param loader
	 *            the class loader
	 * @return the class finder instance
	 */
	public ClassFinder<?> using(ClassLoader loader) {
		this.loader = loader;
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
	@SuppressWarnings("all")
	public ImmutableSet<Class<? extends T>> find() {
		final ImmutableSet.Builder<Class<? extends T>> builder = ImmutableSet.builder();
		final ClassScanner scanner = new ClassScanner(loader, packages.toArray(new String[] {}));

		if (Object.class == type && annotations.isEmpty()) {
			throw new IllegalStateException("please provide some annnotations.");
		}
		if (Object.class == type) {
			for (Class<?> a : annotations) {
				for (Class<?> c : scanner.getTypesAnnotatedWith(a)) {
					builder.add((Class) c);
				}
			}
			return builder.build();
		}
		final Set<Class<? extends T>> all = scanner.getSubTypesOf(type);
		for (Class<? extends T> cls : all) {
			if (hasAnnotation(cls)) {
				builder.add(cls);
			}
		}
		return builder.build();
	}
}