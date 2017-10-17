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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.zip.GZIPOutputStream;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.axelor.app.internal.AppFilter;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;

@Singleton
public class I18nServlet extends HttpServlet {

	private static final long serialVersionUID = -6879530734799286544L;

	private static final String CONTENT_ENCODING = "Content-Encoding";
	private static final String ACCEPT_ENCODING = "Accept-Encoding";
	private static final String GZIP_ENCODING = "gzip";
	private static final String CONTENT_TYPE = "application/javascript; charset=utf8";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		Locale locale = AppFilter.getLocale();
		if (locale == null) {
			locale = req.getLocale();
		}

		final ResourceBundle bundle = I18n.getBundle(locale);
		if (bundle == null) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		resp.setContentType(CONTENT_TYPE);

		OutputStream out = resp.getOutputStream();
		if (req.getHeader(ACCEPT_ENCODING) != null &&
			req.getHeader(ACCEPT_ENCODING).toLowerCase().indexOf(GZIP_ENCODING) > -1) {
			resp.setHeader(CONTENT_ENCODING, GZIP_ENCODING);
			out = new GZIPOutputStream(out);
		}

		final StringBuilder builder = new StringBuilder();
		builder.append("(function() {");
		builder.append("this._t={};_t.bundle=");

		Enumeration<String> keys = bundle.getKeys();
		Map<String, String> messages = new HashMap<>();

		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			messages.put(key, bundle.getString(key));
		}

		try {
			final ObjectMapper mapper = Beans.get(ObjectMapper.class);
			builder.append(mapper.writeValueAsString(messages)).append(";");
			builder.append("}(this));");
		} catch (Exception e) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		out.write(builder.toString().getBytes(StandardCharsets.UTF_8));
		out.close();
	}
}
