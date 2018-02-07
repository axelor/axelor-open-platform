/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.web.tags;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.axelor.app.AppSettings;

public abstract class AbstractTag extends SimpleTagSupport {

	private String src;

	private boolean production = AppSettings.get().isProduction();

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	private boolean exists(String path) {
		try {
			return getResource(path) != null;
		} catch (MalformedURLException e) {
			return false;
		}
	}

	protected URL getResource(String path) throws MalformedURLException {
		final String resource = path.startsWith("/") ? path : "/" + path;
		final PageContext ctx = (PageContext) getJspContext();
		return ctx.getServletContext().getResource(resource);
	}

	protected List<String> getScripts() throws IOException {
		return Arrays.asList(src);
	}

	protected abstract void doTag(String src) throws IOException;

	private boolean gzipSupported() {
		final PageContext context = (PageContext) getJspContext();
		final HttpServletRequest req = (HttpServletRequest) context.getRequest();
		final String encodings = req.getHeader("Accept-Encoding");
		return encodings != null && encodings.toLowerCase().contains("gzip");
	}

	@Override
	public void doTag() throws JspException, IOException {

		if (production) {
			final String gzipped = src.replaceAll("^(js|css)\\/(.*)\\.(js|css)$", "dist/$2.gzip.$3");
			if (exists(gzipped) && gzipSupported()) {
				doTag(gzipped);
				return;
			}
			final String minified = src.replaceAll("^(js|css)\\/(.*)\\.(js|css)$", "dist/$2.min.$3");
			if (exists(minified)) {
				doTag(minified);
				return;
			}
		}

		final List<String> scripts = getScripts();
		if (scripts == null || scripts.isEmpty()) {
			return;
		}

		for (String script : scripts) {
			doTag(script);
		}
	}
}
