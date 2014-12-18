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
package com.axelor.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.CharStreams;

/**
 * Provides helper methods to find version information of axelor projects.
 *
 */
public final class VersionUtils {

	private static Version version;

	private static final String VERSION_FILE = "axelor-common-version.txt";
	private static final String VERSION_GRADLE_FILE = "version.gradle";
	private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:\\-rc(\\d+))?$");
	private static final Pattern VERSION_GRADLE_PATTERN = Pattern.compile("^version\\s+(\"|')(.*?)(\"|')", Pattern.MULTILINE);

	/**
	 * This class stores version details of axelor modules.
	 *
	 */
	public static class Version {

		public final String version;

		// feature version (major.minor)
		public final String feature;

		public final int major;

		public final int minor;

		public final int patch;

		public final int rc;

		Version(String version) {
			final Matcher matcher = VERSION_PATTERN.matcher(version.trim());
			if (!matcher.matches()) {
				throw new IllegalStateException("Invalid version string.");
			}
			this.version = version.trim();
			this.major = Integer.parseInt(matcher.group(1));
			this.minor = Integer.parseInt(matcher.group(2));
			this.patch = Integer.parseInt(matcher.group(3));
			this.rc = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 0;
			this.feature = String.format("%s.%s", major, minor);
		}

		@Override
		public String toString() {
			return version;
		}
	}

	/**
	 * Get the Axelor SDK version.
	 *
	 * @return an instance of {@link Version}
	 */
	public static Version getVersion() {
		if (version == null) {
			version = getVersion(VERSION_FILE);
		}
		return version;
	}

	private static Version getVersion(String file) {
		try (InputStream is = ClassUtils.getResourceStream(file)) {
			String version = CharStreams.toString(new InputStreamReader(is));
			return new Version(version);
		} catch (Exception e) {
		}
		try {
			return fromGradle();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to read version details.");
		}
	}

	private static Version fromGradle() throws IOException {
		File file = Paths.get(VERSION_GRADLE_FILE).toFile();
		if (!file.exists()) {
			file = Paths.get("..", VERSION_GRADLE_FILE).toFile();
		}
		if (!file.exists()) {
			throw new FileNotFoundException("Unable to find version.gradle.");
		}
		final InputStream is = new FileInputStream(file);
		try {
			final String text = CharStreams.toString(new InputStreamReader(is));
			final Matcher matcher = VERSION_GRADLE_PATTERN.matcher(text);
			if (matcher.find()) {
				return new Version(matcher.group(2));
			}
		} finally {
			is.close();
		}
		throw new IOException("Unable to read version.gradle.");
	}
}
