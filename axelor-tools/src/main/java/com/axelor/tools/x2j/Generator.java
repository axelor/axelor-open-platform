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
package com.axelor.tools.x2j;

import java.io.File;
import java.io.IOException;

import org.slf4j.LoggerFactory;

import com.axelor.tools.x2j.pojo.Entity;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;

public class Generator {

	static final String DOMAIN_PATH = "src/main/resources/domains";

	static final String OUTPUT_PATH = "src-gen";

	protected Log log = new Log(LoggerFactory.getLogger(getClass()));

	protected File domainPath;

	protected File outputPath;

	protected File base;
	protected File target;

	public Generator(File base, File target) {
		this.base = base;
		this.domainPath = this.file(base, DOMAIN_PATH);
		this.outputPath = this.file(target, OUTPUT_PATH);
	}

	public Generator(String base, String target) {
		this(new File(base), new File(target));
	}

	public Log getLog() {
		return log;
	}

	protected File file(File base, String... parts) {
		return new File(base.getPath() + "/" + Joiner.on("/").join(parts));
	}

	protected void expand(File input, File outputPath, Entity entity) throws IOException {

		File output = this.file(outputPath, entity.getFile());
		String fileName = output.getPath();

		//TODO: check before parsing
		if (input.lastModified() < output.lastModified()) {
			return;
		}

		output.getParentFile().mkdirs();

		String[] existing = {
			entity.getName() + ".java",
			entity.getName() + ".groovy"
		};

		for (String name : existing) {
			File ex = this.file(output.getParentFile(), name);
			if (ex.exists()) {
				ex.delete();
			}
		}

		log.info("Generating: " + fileName);

		String code = Expander.expand(entity);

		Files.write(code, output, Charsets.UTF_8);
	}

	protected void process(File input, File outputPath) throws IOException {

		log.info("Processing: " + input);

		for (Entity it : XmlHelper.entities(input)) {
			expand(input, outputPath, it);
		}
	}

	private void delete(File file) {
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				delete(f);
			}
		}
		file.delete();
	}

	public void clean() {

		if (!this.outputPath.exists()) return;

		log.info("Cleaning generated sources.");
		log.info("Output path: " + outputPath);

		for (File file : outputPath.listFiles()) {
			delete(file);
		}
	}

	public void start() throws IOException {

		log.info("Generating JPA classes.");
		log.info("Domain path: " + domainPath);
		log.info("Output path: " + outputPath);

		outputPath.mkdirs();

		for (File file : domainPath.listFiles()) {
			if (file.getName().endsWith(".xml")) {
				process(file, outputPath);
			}
		}
	}
}
