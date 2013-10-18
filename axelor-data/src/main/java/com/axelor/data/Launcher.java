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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;

public abstract class Launcher {

	protected final Logger LOG = LoggerFactory.getLogger(getClass());

	/**
	 * Create additional guice module that configures other stuffs like
	 * persistence etc.
	 *
	 */
	protected abstract AbstractModule createModule();

	public void run(String... args) throws IOException {

		Commander cmd = new Commander();
		try {
			if (args == null || args.length == 0)
				throw new Exception();
			cmd.parse(args);
			if (!cmd.getDataDir().isDirectory())
				throw new Exception("invalid data directory");
			if (!cmd.getConfig().isFile())
				throw new Exception("invalid config file");
		} catch (Exception e) {
			String message = e.getMessage();
			if (!Strings.isNullOrEmpty(message))
				System.err.println(e.getMessage());
			Commander.usage();
			return;
		}

		if (cmd.getShowHelp() == Boolean.TRUE) {
			Commander.usage();
			return;
		}

		final String config = cmd.getConfig().getPath();
		final String dataDir = cmd.getDataDir().getPath();
		final String errorDir = cmd.getErrorDir() == null ? null : cmd.getErrorDir().getPath();

		Injector injector = Guice.createInjector(new AbstractModule() {

			@Override
			protected void configure() {
				install(createModule());
				bindConstant().annotatedWith(Names.named("axelor.data.config")).to(config);
				bindConstant().annotatedWith(Names.named("axelor.data.dir")).to(dataDir);
				bind(String.class).annotatedWith(Names.named("axelor.error.dir")).toProvider(Providers.<String>of(errorDir));
			}
		});

		if (LOG.isInfoEnabled())
			LOG.info("Importing data. Please wait...");

		Importer importer = injector.getInstance(Importer.class);
		Map<String, String[]> mappings = new HashMap<String, String[]>();

		for (Map.Entry<Object, Object> entry : cmd.getFiles().entrySet()) {
			String name = (String) entry.getKey();
			String[] files = ((String) entry.getValue()).split(",");
			mappings.put(name, files);
		}

		importer.run(mappings);

		if (LOG.isInfoEnabled())
			LOG.info("Import done!");
	}
}
