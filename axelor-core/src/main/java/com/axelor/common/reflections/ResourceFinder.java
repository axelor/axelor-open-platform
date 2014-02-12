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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;

/**
 * The {@link ResourceFinder} class provides fluent api to search for
 * resources.
 * 
 */
public final class ResourceFinder {
	
	private ClassLoader loader;
	private Set<Pattern> namePatterns = Sets.newLinkedHashSet();
	private Set<Pattern> pathPatterns = Sets.newLinkedHashSet();
	
	ResourceFinder() {
	}

	/**
	 * Find using the given {@link ClassLoader}
	 * 
	 * @param loader
	 *            the class loader to use
	 * @return the same finder
	 */
	public ResourceFinder using(ClassLoader loader) {
		this.loader = loader;
		return this;
	}
	
	/**
	 * Find with the given name pattern.
	 * 
	 * @param pattern
	 *            the name pattern
	 * @return the same finder
	 */
	public ResourceFinder byName(String pattern) {
		Preconditions.checkNotNull(pattern, "pattern must not be null");
		namePatterns.add(Pattern.compile(pattern));
		return this;
	}
	
	/**
	 * Find with the given URL pattern.
	 * 
	 * @param pattern
	 *            the URL pattern
	 * @return the same finder
	 */
	public ResourceFinder byURL(String pattern) {
		Preconditions.checkNotNull(pattern, "pattern must not be null");
		pathPatterns.add(Pattern.compile(pattern));
		return this;
	}

	/**
	 * Search the resources by full pattern match using
	 * {@link Matcher#matches()} call.
	 * 
	 * @return list of URL objects
	 */
	public ImmutableList<URL> match() {
		return find(false);
	}

	/**
	 * Search the resources by partial match using {@link Matcher#find()}
	 * call.
	 * 
	 * @return list of URL objects
	 */
	public ImmutableList<URL> find() {
		return find(true);
	}
	
	private ImmutableList<URL> find(boolean partial) {
		ImmutableList.Builder<URL> all = ImmutableList.builder();
		for (Pattern namePattern : namePatterns) {
			for (URL file : getResources(namePattern, loader, partial)) {
				if (pathPatterns.isEmpty()) {
					all.add(file);
					continue;
				}
				for (Pattern pathPattern : pathPatterns) {
					Matcher matcher = pathPattern.matcher(file.getFile());
					boolean matched = partial ? matcher.find() : matcher.matches();
					if (matched) {
						all.add(file);
					}
				}
			}
		}
		return all.build();
	}

	private static ImmutableList<URL> getResources(Pattern pattern, ClassLoader loader, boolean partial) {
		final ImmutableList.Builder<URL> builder = ImmutableList.builder();
		final ClassLoader classLoader = loader == null ? Thread.currentThread().getContextClassLoader() : loader;
		try {
			for (ResourceInfo info : ClassPath.from(classLoader).getResources()) {
				String name = info.getResourceName();
				Matcher matcher = pattern.matcher(name);
				boolean matched = partial ? matcher.find() : matcher.matches();
				if (matched) {
					Enumeration<URL> urls = classLoader.getResources(name);
					while (urls.hasMoreElements()) {
						builder.add(urls.nextElement());
					}
				}
			}
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
		return builder.build();
	}
}