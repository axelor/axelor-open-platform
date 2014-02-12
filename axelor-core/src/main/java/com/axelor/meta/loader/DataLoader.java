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

import javax.inject.Inject;
import javax.inject.Singleton;

import com.axelor.common.FileUtils;
import com.axelor.data.csv.CSVImporter;
import com.axelor.data.xml.XMLImporter;
import com.axelor.meta.MetaScanner;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.LineReader;
import com.google.inject.Injector;

@Singleton
class DataLoader extends AbstractLoader {

	private static final String DATA_DIR_NAME = "data-init";

	private static final String INPUT_DIR_NAME = "input";
	private static final String INPUT_CONFIG_NAME = "input-config.xml";

	private static Pattern patCsv = Pattern.compile("^\\<\\s*csv-inputs");
	private static Pattern patXml = Pattern.compile("^\\<\\s*xml-inputs");

	@Inject
	private Injector injector;
	
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
		CSVImporter importer = new CSVImporter(injector, config.getAbsolutePath(), data.getAbsolutePath(), null);
		try {
			importer.run(null);
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}

	private void importXml(File config) {
		File data = FileUtils.getFile(config.getParentFile(), INPUT_DIR_NAME);
		XMLImporter importer = new XMLImporter(injector, config.getAbsolutePath(), data.getAbsolutePath());
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
