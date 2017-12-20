/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.web;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.app.AppSettings;
import com.axelor.db.search.SearchService;
import com.axelor.db.tenants.TenantModule;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.quartz.JobRunner;

@Singleton
public class AppInitializer extends HttpServlet {

	private static final long serialVersionUID = -2493577642638670615L;

	private static final Logger LOGGER = LoggerFactory.getLogger(AppInitializer.class);

	@Inject
	private ModuleManager moduleManager;

	@Inject
	private JobRunner jobRunner;

	@Inject
	private SearchService searchService;
	
	@Override
	public void init() throws ServletException {
		LOGGER.info("Initializing...");

		try {
			moduleManager.initialize(false, AppSettings.get().getBoolean("data.import.demo-data", true));
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		// initialize search index
		if (searchService.isEnabled()) {
			try {
				searchService.createIndex(false);
			} catch (InterruptedException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}

		try {
			if (jobRunner.isEnabled()) {
				if (TenantModule.isEnabled()) {
					LOGGER.info("Scheduler is not supported in multi-tenant mode.");
				} else {
					jobRunner.start();
				}
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		LOGGER.info("Ready to serve...");
	}

	@Override
	public void destroy() {
		try {
			jobRunner.stop();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
}
