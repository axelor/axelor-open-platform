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
