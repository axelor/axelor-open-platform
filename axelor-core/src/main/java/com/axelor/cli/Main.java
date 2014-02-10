package com.axelor.cli;

import java.util.Properties;

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
		}
	}
	
	public static void main(String[] args) {
		
		Options opts = new Options();
		JCommander cmd = new JCommander(opts);

		cmd.setProgramName("axelor");
		
		try {
			cmd.parse(args);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			cmd.usage();
			return;
		}

		if ((opts.showHelp == Boolean.TRUE)
				|| (opts.init == Boolean.FALSE && opts.update == Boolean.FALSE)) {
			cmd.usage();
			return;
		}
		
		Injector injector = Guice.createInjector(new MainModule(opts.unit));
		ModuleManager manager = injector.getInstance(ModuleManager.class);
		
		if (opts.init == Boolean.TRUE) {
			manager.initialize(opts.update == Boolean.TRUE, opts.importDemo == Boolean.TRUE);
			return;
		}

		String[] names = {};
		if (opts.modules != null) {
			names = opts.modules.toArray(new String[] {});
		}
		manager.update(opts.importDemo == Boolean.TRUE, names);
	}
}
