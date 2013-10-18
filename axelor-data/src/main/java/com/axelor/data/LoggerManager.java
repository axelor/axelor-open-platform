/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.data;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.axelor.data.csv.CSVInput;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

/**
 * This {@link LoggerManager} class logs all errors during the import into a specific file.
 *
 * @author axelor
 *
 */
public class LoggerManager {

	private File errorDir;

	private CSVInput input;

	private String[] header;

	private File currentFile;

	private List<String> filesName = Lists.newArrayList();

	/**
	 * Main constructor
	 * @param directory that will contain the files
	 */
	public LoggerManager(String dir) {
		this.errorDir = this.computeDir(dir);
	}

	/**
	 * Get the directory where errors will be logs. Takes care to delete the directory.
	 * @param directory that will contain the files
	 * @return the directory
	 */
	private File computeDir(String errorDir) {
		if(Strings.isNullOrEmpty(errorDir)) {
			return null;
		}

		return new File(Files.simplifyPath(errorDir));
	}

	/**
	 * Log the row into a specific file.
	 * @param values
	 * @param csvBinder
	 * @param csvInput
	 */
	public void log(String[] values) {
		if(this.errorDir == null || this.currentFile == null || this.input == null) {
			return ;
		}

		try {
			if(!this.currentFile.exists()) {
				Files.createParentDirs(currentFile);
				Files.append(Joiner.on(this.input.getSeparator()).join(this.header), this.currentFile, Charsets.UTF_8);
				this.filesName.add(this.currentFile.getName());
			}

			Files.append("\n" + Joiner.on(this.input.getSeparator()).join(values), this.currentFile, Charsets.UTF_8);
		} catch (IOException e) {
		}
	}

	/**
	 * Configures the file that will receive the rows in error
	 * @param csvInput
	 * @param fields
	 */
	public void prepareInput(CSVInput csvInput, String[] fields) {
		this.header = fields;
		this.input = csvInput;
		this.currentFile = this.getCurrentFile(csvInput.getFileName());

		if(this.currentFile.exists()) {
			this.currentFile.delete();
		}
	}

	/**
	 * Return an unique file (not already created)
	 * @return file
	 */
	private File getCurrentFile(String fileName) {
		if(!this.filesName.contains(this.input.getFileName())) {
			return new File(this.errorDir, this.input.getFileName());
		}
		return new File(this.errorDir, getCurrentFile(this.input.getFileName(), 1));
	}

	/**
	 * Recursive method that determinate an unique file name
	 * @param fileName
	 * @param level
	 * @return
	 */
	private String getCurrentFile(String fileName, int level) {
		String name = this.input.getFileName().replace(".csv", "").concat("_" + level).concat(".csv");
		if(!this.filesName.contains(name)) {
			return name;
		}
		return getCurrentFile(fileName, level++);
	}

}
