/**
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
package com.axelor.web.servlet;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.app.AppSettings;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.quartz.JobRunner;

@Singleton
public class InitServlet extends HttpServlet {

	private static final long serialVersionUID = -2493577642638670615L;

	private static final Logger LOG = LoggerFactory.getLogger(InitServlet.class);

	@Inject
	private ModuleManager moduleManager;
	
	@Inject
	private JobRunner jobRunner;
	
	@Override
	public void init() throws ServletException {
		LOG.info("Initializing...");

		try {
			moduleManager.initialize(false, AppSettings.get().getBoolean("data.import.demo-data", true));
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}

		try {
			if (jobRunner.isEnabled()) {
				jobRunner.start();
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		
		super.init();
	}
	
	@Override
	public void destroy() {
		try {
			jobRunner.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.destroy();
	}
}
