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
package com.axelor.cli;

import java.util.Properties;

import com.axelor.app.AppModule;
import com.axelor.auth.AuthModule;
import com.axelor.db.JpaModule;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.rpc.ObjectMapperProvider;
import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main {

	private static final String PROGRAM_NAME = "axelor";

	static class MainModule extends AbstractModule {

		protected Properties properties = new Properties();

		private String jpaUnit;

		public MainModule(String jpaUnit) {
			this.jpaUnit = jpaUnit;
		}

		@Override
		protected void configure() {

			bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class);

			install(new JpaModule(jpaUnit).properties(properties));
			install(new AuthModule.Simple().properties(properties));
			install(new AppModule());
		}
	}

	public static void main(String[] args) {
		System.exit(process(args));
	}

	public static Integer process(String[] args) {

		Options opts = new Options();
		JCommander cmd = new JCommander(opts);

		cmd.setProgramName(PROGRAM_NAME);

		try {
			cmd.parse(args);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			cmd.usage();
			return -1;
		}

		if ((opts.showHelp == Boolean.TRUE)
				|| (opts.init == Boolean.FALSE && opts.update == Boolean.FALSE)) {
			cmd.usage();
			return 0;
		}

		Injector injector = Guice.createInjector(new MainModule(opts.unit));
		ModuleManager manager = injector.getInstance(ModuleManager.class);

		if (opts.init == Boolean.TRUE) {
			manager.initialize(opts.update == Boolean.TRUE, opts.importDemo == Boolean.TRUE);
			return 0;
		}

		String[] names = {};
		if (opts.modules != null) {
			names = opts.modules.toArray(new String[] {});
		}
		manager.update(opts.importDemo == Boolean.TRUE, names);
		return 0;
	}
}
