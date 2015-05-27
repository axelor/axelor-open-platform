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
package com.axelor.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.report.data.oda.jdbc.IConnectionFactory;
import org.eclipse.birt.report.engine.api.EngineConstants;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.birt.report.model.api.IResourceLocator;
import org.hibernate.Session;
import org.hibernate.internal.SessionFactoryImpl;

import com.axelor.app.internal.AppFilter;
import com.axelor.db.JPA;
import com.google.common.base.Throwables;

public class ReportGenerator {

	@Inject
	private IReportEngine engine;

	public void generate(OutputStream output, String designName, String format,
			Map<String, Object> params) throws IOException, BirtException {
		generate(output, designName, format, params, AppFilter.getLocale());
	}

	@SuppressWarnings("unchecked")
	public void generate(OutputStream output, String designName, String format,
			Map<String, Object> params, Locale locale) throws IOException, BirtException {

		final IResourceLocator locator = engine.getConfig().getResourceLocator();
		final URL found = locator.findResource(null, designName, IResourceLocator.OTHERS);

		if (found == null) {
			throw new BirtException("No such report found: " + designName);
		}

		final InputStream stream = found.openStream();
		try {

			final IReportRunnable report = engine.openReportDesign(designName, stream);
			final IRunAndRenderTask task = engine.createRunAndRenderTask(report);
			final IRenderOption opts = new RenderOption();

			opts.setOutputFormat(format);
			opts.setOutputStream(output);

			task.setLocale(locale);
			task.setRenderOption(opts);
			task.setParameterValues(params);

			Session session = JPA.em().unwrap(Session.class);
			SessionFactoryImpl sessionFactory = (SessionFactoryImpl) session.getSessionFactory();
			Connection connection = null;
			try {
				connection = sessionFactory.getConnectionProvider().getConnection();
			} catch (SQLException e) {
				throw Throwables.propagate(e);
			}

			task.getAppContext().put(EngineConstants.APPCONTEXT_CLASSLOADER_KEY, getClass().getClassLoader());
			task.getAppContext().put(IConnectionFactory.PASS_IN_CONNECTION, connection);
			task.getAppContext().put(IConnectionFactory.CLOSE_PASS_IN_CONNECTION, Boolean.FALSE);

			try {
				task.run();
			} finally {
				task.close();
			}
		} finally {
			stream.close();
		}
	}
}
