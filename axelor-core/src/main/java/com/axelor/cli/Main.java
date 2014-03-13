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
		int status = 0;
		try {
			status = process(args);
		} catch (Exception e) {
			status = -1;
		}
		System.exit(status);
	}

	public static int process(String[] args) {

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
