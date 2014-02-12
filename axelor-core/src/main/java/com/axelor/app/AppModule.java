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
package com.axelor.app;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.common.reflections.Reflections;
import com.axelor.meta.loader.ModuleManager;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;

/**
 * The application module scans the classpath and finds all the
 * {@link AxelorModule} and installs them in the dependency order.
 * 
 */
public class AppModule extends AbstractModule {

	private static Logger log = LoggerFactory.getLogger(AppModule.class);

	private List<Class<? extends AxelorModule>> findAll() {
		
		final List<String> mods = ModuleManager.findInClassPath(false);
		final List<Class<? extends AxelorModule>> all = Lists.newArrayList();
		
		for (Class<? extends AxelorModule> module : Reflections
				.findSubTypesOf(AxelorModule.class)
				.having(AxelorModuleInfo.class)
				.find()) {
			String name = module.getAnnotation(AxelorModuleInfo.class).name();
			if (mods.contains(name)) {
				all.add(module);
			}
		}
		
		Collections.sort(all, new Comparator<Class<?>>() {
			
			@Override
			public int compare(Class<?> o1, Class<?> o2) {
				String n1 = o1.getAnnotation(AxelorModuleInfo.class).name();
				String n2 = o2.getAnnotation(AxelorModuleInfo.class).name();
				int idx1 = mods.indexOf(n1);
				int idx2 = mods.indexOf(n2);
				return idx1 - idx2;
			}
		});
				
		return all;
	}
	
	@Override
	protected void configure() {
		
		final List<Class<? extends AxelorModule>> all = findAll();
		if (all.isEmpty()) {
			return;
		}
		
		log.info("Configuring app modules...");
		for (Class<? extends AxelorModule> module : all) {
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
