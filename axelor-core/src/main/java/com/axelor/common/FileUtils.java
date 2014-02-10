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
