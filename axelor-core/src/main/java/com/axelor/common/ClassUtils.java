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
import java.net.URL;

/**
 * The class provides static helper methods to deal with classes & resource.
 * 
 */
public final class ClassUtils {

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
