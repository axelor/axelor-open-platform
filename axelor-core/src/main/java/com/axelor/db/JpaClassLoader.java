/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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

import java.net.URL;
import java.net.URLClassLoader;

/**
 * The class loader for domain objects.
 *
 * @todo implement code enhancer
 * @todo implement dynamic fields
 * @todo implement class reload
 */
public class JpaClassLoader extends URLClassLoader {

	public JpaClassLoader() {
		super(new URL[0], Thread.currentThread().getContextClassLoader());
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		try {
			return super.findClass(name);
		} catch (ClassNotFoundException e) {
			Class<?> model = findModelClass(name);
			if (model != null)
				return model;
			throw e;
		}
	}

	/**
	 * try to find domain class in cache.
	 *
	 */
	private Class<?> findModelClass(String name) {
		if (!name.startsWith("java.lang.") || name.contains("$"))
			return null;
		return JpaScanner.findModel(name.substring(name.lastIndexOf('.') + 1));
	}
}
