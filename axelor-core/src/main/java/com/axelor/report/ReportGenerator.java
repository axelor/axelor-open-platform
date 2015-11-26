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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.report.data.oda.jdbc.IConnectionFactory;
import org.eclipse.birt.report.engine.api.EngineConstants;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.birt.report.engine.emitter.pdf.PDFPageDevice;
import org.eclipse.birt.report.model.api.IResourceLocator;
import org.hibernate.Session;
import org.hibernate.internal.SessionFactoryImpl;

import com.axelor.app.internal.AppFilter;
import com.axelor.db.JPA;
import com.axelor.meta.MetaFiles;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

/**
 * The report generator service.
 *
 */
public class ReportGenerator {

	@Inject
	private IReportEngine engine;

	/**
	 * Generate a report for the given report design.
	 *
	 * @param output
	 *            the report output stream
	 * @param designName
	 *            report design
	 * @param format
	 *            output format (e.g. pdf, html etc)
	 * @param params
	 *            report parameters
	 * @throws IOException
	 * @throws BirtException
	 */
	public void generate(OutputStream output, String designName, String format, Map<String, Object> params)
			throws IOException, BirtException {
		generate(output, designName, format, params, AppFilter.getLocale());
	}

	/**
	 * Generate a report for the given report design.
	 *
	 * @param output
	 *            the report output stream
	 * @param designName
	 *            report design
	 * @param format
	 *            output format (e.g. pdf, html etc)
	 * @param params
	 *            report parameters
	 * @param locale
	 *            report output locale
	 * @throws IOException
	 * @throws BirtException
	 */
	@SuppressWarnings("unchecked")
	public void generate(OutputStream output, String designName, String format, Map<String, Object> params,
			Locale locale) throws IOException, BirtException {

		final IResourceLocator locator = engine.getConfig().getResourceLocator();
		final URL found = locator.findResource(null, designName, IResourceLocator.OTHERS);

		if (found == null) {
			throw new BirtException("No such report found: " + designName);
		}

		try (InputStream stream = found.openStream()) {

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
		}
	}

	/**
	 * Generate a report to a temporary file and return path to the generated
	 * file.
	 *
	 * @param designName
	 *            report design name
	 * @param format
	 *            output format
	 * @param params
	 *            report parameters
	 * @param locale
	 *            report output language
	 * @return {@link Path} to the generated file
	 * @throws IOException
	 * @throws BirtException
	 */
	public File generate(String designName, String format, Map<String, Object> params, Locale locale)
			throws IOException, BirtException {
		Preconditions.checkNotNull(designName, "no report design name given");
		final Path tmpFile = MetaFiles.createTempFile(null, "");
		final FileOutputStream stream = new FileOutputStream(tmpFile.toFile());

		generate(stream, designName, format, params);

		return tmpFile.toFile();
	}

	static {
		// BIRT when used as embedded library, shows full file path as version,
		// this is security risk as this information is exposed in pdf metadata
		try {
			final Field field = PDFPageDevice.class.getDeclaredField("versionInfo");
			field.setAccessible(true);
			final String[] info = (String[]) field.get(PDFPageDevice.class);
			final String version = info[0];
			if (version.endsWith(".jar")) {
				final Pattern p = Pattern.compile(".*?\\-([^-]+)\\.jar");
				final Matcher m = p.matcher(version);
				if (m.matches()) {
					info[0] = m.group(1);
				}
			}
		} catch (Exception e) {
		}
	}
}
