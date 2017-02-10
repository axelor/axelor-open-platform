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
package com.axelor.inject;

import java.lang.annotation.Annotation;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.Injector;
import com.google.inject.Key;

/**
 * A singleton class that can be used to get instances of injetable services
 * where injection is not possible.
 *
 */
@Singleton
public final class Beans {

	private static Beans instance;

	private Injector injector;

	@Inject
	private Beans(Injector injector) {
		this.injector = injector;
		instance = this;
	}

	private static Beans get() {
		if (instance == null || instance.injector == null) {
			throw new RuntimeException("Guice is not initialized.");
		}
		return instance;
	}

	/**
	 * Returns the appropriate instance for the given injection type.
	 *
	 * @param <T>
	 *            type of the requested bean
	 * @param type
	 *            the requested type
	 * @return an appropriate instance of the given type
	 */
	public static <T> T get(Class<T> type) {
		return get().injector.getInstance(type);
	}

	/**
	 * Returns the appropriate instance for the given injection type qualified
	 * by the given annotation.
	 * 
	 * @param <T>
	 *            type of the requested bean
	 * @param type
	 *            the requested type
	 * @param qualifier
	 *            the qualifier annotation
	 * @return an appropriate instance of the given type
	 */
	public static <T> T get(Class<T> type, Annotation qualifier) {
		return get().injector.getInstance(Key.get(type, qualifier));
	}

	/**
	 * Injects dependencies into the fields and methods of {@code bean}.
	 *
	 * @param <T>
	 *            type of the bean
	 * @param bean
	 *            to inject members on
	 * @return the bean itself
	 */
	public static <T> T inject(T bean) {
		get().injector.injectMembers(bean);
		return bean;
	}
}
