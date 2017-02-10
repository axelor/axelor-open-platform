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
package com.axelor.common.reflections;

import java.lang.annotation.Annotation;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * The helper class to find sub types of a given super class.
 * 
 */
public final class ClassFinder<T> {

	private Class<T> type;
	private ClassLoader loader;

	private Set<Class<? extends Annotation>> annotations = Sets.newLinkedHashSet();
	private Set<String> packages = Sets.newLinkedHashSet();
	private Set<String> pathPatterns = Sets.newLinkedHashSet();

	private boolean matchAll = true;

	ClassFinder(Class<T> type, ClassLoader loader) {
		this.type = type;
		this.loader = loader;
	}

	ClassFinder(Class<T> type) {
		this(type, Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Find with the given URL pattern.
	 *
	 * @param pattern
	 *            the URL pattern
	 * @return the same finder
	 */
	public ClassFinder<T> byURL(String pattern) {
		Preconditions.checkNotNull(pattern, "pattern must not be null");
		pathPatterns.add(pattern);
		return this;
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

		for (String pattern : pathPatterns) {
			scanner.byURL(pattern);
		}

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