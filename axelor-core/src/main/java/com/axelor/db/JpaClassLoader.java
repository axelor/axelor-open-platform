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
