/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
	 * try to find domain or repository class in cache.
	 *
	 */
	private Class<?> findModelClass(String className) {
		if (!className.startsWith("java.lang.") || className.contains("$"))
			return null;
		String name = className.substring(className.lastIndexOf('.') + 1);
		Class<?> klass = JpaScanner.findRepository(name);
		if (klass == null) {
			return JpaScanner.findModel(name);
		}
		return klass;
	}
}
