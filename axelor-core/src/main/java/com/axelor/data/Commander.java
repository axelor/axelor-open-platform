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
package com.axelor.data;

import java.io.File;
import java.util.List;
import java.util.Properties;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.Lists;

/**
 * This {@link Commander} class parse the command line and assign the fields with the correct values.
 * @author axelor
 *
 */
public class Commander {

	@Parameter(required = true, names = { "-c", "--config" }, description = "data binding configuration file")
	private File config;

	@Parameter(required = true, names = { "-d", "--data-dir" }, description = "location of data directory")
	private File dataDir;

	@Parameter( names = { "-e", "--error-dir" }, description = "location of error directory")
	private File errorDir;

	@DynamicParameter(names = "-D", description = "define input mappings")
	private Properties files = new Properties();

	@Parameter( names = { "-h", "--help" }, description = "show this help message")
	private Boolean showHelp;

	public void setConfig(File config) {
		this.config = config;
	}

	public File getConfig() {
		return config;
	}

	public File getDataDir() {
		return dataDir;
	}

	public File getErrorDir() {
		return errorDir;
	}

	public Properties getFiles() {
		return files;
	}

	public Boolean getShowHelp() {
		return showHelp;
	}

	public File getFile(String name) {
		return new File(dataDir, name);
	}

	public List<File> getFiles(String key) {
		List<File> all = Lists.newArrayList();
		if (files.containsKey(key))
			for (String file : files.get(key).toString().split(","))
				all.add(new File(dataDir, file));
		return all;
	}

	public void setFiles(Properties files) {
		this.files = files;
	}

	/**
	 * Parse the command line args
	 * @param args
	 * @return the configured jcommander
	 */
	public JCommander parse(String... args) {
		JCommander commander = getCommander(this);
		commander.parse(args);
		return commander;
	}

	/**
	 * Configure the {@link JCommander}
	 * @param cmd
	 * @return the configured jcommander
	 */
	private static JCommander getCommander(Commander cmd) {
		JCommander commander = new JCommander(cmd);
		commander.setProgramName("axelor-data");
		return commander;
	}

	/**
	 * Display the help on System.out.
	 */
	public static void usage() {
		Commander cmd = new Commander();
		cmd.setFiles(null);
		getCommander(cmd).usage();
	}

}
