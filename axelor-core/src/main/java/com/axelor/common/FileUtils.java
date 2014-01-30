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
