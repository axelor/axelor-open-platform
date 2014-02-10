package com.axelor.meta.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reflections.vfs.Vfs;

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
class DataLoader implements Loader {

	private static final String DATA_DIR_NAME = "data-init";

	private static final String INPUT_DIR_NAME = "input";
	private static final String INPUT_CONFIG_NAME = "input-config.xml";

	private static Pattern patCsv = Pattern.compile("^\\<\\s*csv-inputs");
	private static Pattern patXml = Pattern.compile("^\\<\\s*xml-inputs");

	@Inject
	private Injector injector;
	
	@Override
	public void load(Module module) {

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
		final List<Vfs.File> files = MetaScanner.findAll(module.getName(), dirName, "(.*?)\\.(xml|csv)");

		if (files.isEmpty()) {
			return null;
		}

		final File tmp = Files.createTempDir();

		for (Vfs.File file : files) {
			String name = file.toString();
			name = name.substring(name.lastIndexOf(dirName));
			try {
				copy(file.openInputStream(), tmp, name);
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
