/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
package com.axelor.app;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.report.engine.api.IReportEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.common.reflections.ClassFinder;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.report.ReportEngineProvider;
import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;

/**
 * The application module scans the classpath and finds all the
 * {@link AxelorModule} and installs them in the dependency order.
 * 
 */
public class AppModule extends AbstractModule {

	private static Logger log = LoggerFactory.getLogger(AppModule.class);

	private List<Class<? extends AxelorModule>> findAll() {
		final List<Class<? extends AxelorModule>> all = new ArrayList<>();
		for (Class<? extends AxelorModule> module : MetaScanner
				.findSubTypesOf(AxelorModule.class)
				.find()) {
			all.add(module);
		}
		return all;
	}
	
	@Override
	protected void configure() {

		// initialize Beans helps
		bind(Beans.class).asEagerSingleton();

		// report engine
		bind(IReportEngine.class).toProvider(ReportEngineProvider.class);

		final List<Class<? extends AxelorModule>> all = findAll();
		if (all.isEmpty()) {
			return;
		}

		final Map<String, URL> modulePaths = ModuleManager.findInstalled();
		if (modulePaths.isEmpty()) {
			return;
		}

		final List<Class<? extends AxelorModule>> moduleClasses = new ArrayList<>();

		for (String name : modulePaths.keySet()) {
			final URL url = modulePaths.get(name);
			final String path = url.getPath().replaceFirst("module\\.properties$", "");
			final String pattern = String.format("^(%s).*", path);
			final ClassFinder<AxelorModule> finders = MetaScanner
					.findSubTypesOf(AxelorModule.class)
					.byURL(pattern);
			for (Class<? extends AxelorModule> klass : finders.find()) {
				moduleClasses.add(klass);
			}
		}

		if (moduleClasses.isEmpty()) {
			return;
		}

		log.info("Configuring app modules...");

		for (Class<? extends AxelorModule> module : moduleClasses) {
			try {
				log.info("Configure: {}", module.getName());
				install(module.newInstance());
			} catch (InstantiationException | IllegalAccessException e) {
				throw Throwables.propagate(e);
			}
		}
		log.info("App modules configured.");
	}
}
