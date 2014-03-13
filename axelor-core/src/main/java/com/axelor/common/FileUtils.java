/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
package com.axelor.common;

import java.io.File;

import com.google.common.base.Preconditions;

/**
 * This class provides some helper methods to deal with files.
 * 
 */
public final class FileUtils {

	/**
	 * Get a file from the given path elements.
	 * 
	 * @param first
	 *            the first path element
	 * @param more
	 *            the additional path elements
	 * @return the file
	 */
	public static File getFile(String first, String... more) {
		Preconditions.checkNotNull(first, "first element must not be null");
		File file = new File(first);
		if (more != null) {
			for (String name : more) {
				file = new File(file, name);
			}
		}
		return file;
	}

	/**
	 * Get a file from the given path elements.
	 * 
	 * @param directory
	 *            the parent directory
	 * @param next
	 *            next path element
	 * @param more
	 *            additional path elements
	 * @return the file
	 */
	public static File getFile(File directory, String next, String... more) {
		Preconditions.checkNotNull(directory, "directory must not be null");
		Preconditions.checkNotNull(next, "next element must not be null");
		File file = new File(directory, next);
		if (more != null) {
			for (String name : more) {
				file = new File(file, name);
			}
		}
		return file;
	}
}
