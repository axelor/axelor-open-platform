/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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

/**
 * The {@link Reflections} utilities provides fast and easy way to search for
 * resources and types.
 * 
 */
public final class Reflections {

	private Reflections() {
	}

	/**
	 * Return a {@link ClassFinder} to search for the sub types of the given
	 * type.
	 * 
	 * @param type
	 *            the super type
	 * @return an instance of {@link ClassFinder}
	 */
	public static <T> ClassFinder<T> findSubTypesOf(Class<T> type) {
		return new ClassFinder<>(type);
	}

	/**
	 * Return a {@link ClassFinder} to search for types.
	 * 
	 * @return an instance of {@link ClassFinder}
	 */
	public static ClassFinder<?> findTypes() {
		return findSubTypesOf(Object.class);
	}

	/**
	 * Return a {@link ResourceFinder} to search for resources.
	 * 
	 * @return an instance of {@link ResourceFinder}
	 */
	public static ResourceFinder findResources() {
		return new ResourceFinder();
	}
}
