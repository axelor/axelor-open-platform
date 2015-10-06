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
package com.axelor.meta.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import com.axelor.common.FileUtils;
import com.axelor.data.csv.CSVImporter;
import com.axelor.data.xml.XMLImporter;
import com.axelor.meta.MetaScanner;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.LineReader;

@Singleton
class DataLoader extends AbstractLoader {

	private static final String DATA_DIR_NAME = "data-init";

	private static final String INPUT_DIR_NAME = "input";
	private static final String INPUT_CONFIG_NAME = "input-config.xml";

	private static Pattern patCsv = Pattern.compile("^\\<\\s*csv-inputs");
	private static Pattern patXml = Pattern.compile("^\\<\\s*xml-inputs");

	@Override
	protected void doLoad(Module module, boolean update) {

		File tmp = extract(module);
		if (tmp == null) {
			return;
		}

		try {
			File config = FileUtils.getFile(tmp, getDirName(), INPUT_CONFIG_NAME);
			if (isConfig(config, patCsv)) {
				importCsv(config);
			} else if (isConfig(config, patXml)) {
				importXml(config);
			}
		} finally {
			clean(tmp);
		}
	}
	
	private void importCsv(File config) {
		File data = FileUtils.getFile(config.getParentFile(), INPUT_DIR_NAME);
		CSVImporter importer = new CSVImporter(config.getAbsolutePath(), data.getAbsolutePath(), null);
		try {
			importer.run(null);
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}

	private void importXml(File config) {
		File data = FileUtils.getFile(config.getParentFile(), INPUT_DIR_NAME);
		XMLImporter importer = new XMLImporter(config.getAbsolutePath(), data.getAbsolutePath());
		importer.run(null);
	}

	private boolean isConfig(File file, Pattern pattern) {
		try {
			Reader reader = new FileReader(file);
			LineReader lines = new LineReader(reader);
			String line = null;
			while ((line = lines.readLine()) != null) {
				if (pattern.matcher(line).find()) {
					return true;
				}
			}
			reader.close();
		} catch (IOException e) {
		}
		return false;
	}
	
	protected String getDirName() {
		return DATA_DIR_NAME;
	}

	private File extract(Module module) {

		final String dirName = this.getDirName();
		final List<URL> files = MetaScanner.findAll(module.getName(), dirName, "(.*?)\\.(xml|csv)");

		if (files.isEmpty()) {
			return null;
		}

		final File tmp = Files.createTempDir();

		for (URL file : files) {
			String name = file.toString();
			name = name.substring(name.lastIndexOf(dirName));
			try {
				copy(file.openStream(), tmp, name);
			} catch (IOException e) {
				throw Throwables.propagate(e);
			}
		}
		
		return tmp;
	}
	
	private void copy(InputStream in, File toDir, String name) throws IOException {
		File dst = FileUtils.getFile(toDir, name);
		Files.createParentDirs(dst);
		OutputStream out = new FileOutputStream(dst);
		try {
			ByteStreams.copy(in, out);
		} finally {
			out.close();
		}
	}

	private void clean(File file) {
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				clean(child);
			}
			file.delete();
		} else if (file.exists()) {
			file.delete();
		}
	}
}
