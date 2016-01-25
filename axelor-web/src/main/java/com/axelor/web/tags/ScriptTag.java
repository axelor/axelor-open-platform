/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspWriter;

import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;

public class ScriptTag extends AbstractTag {
	
	private List<String> files(final String manifest) throws IOException {

		final List<String> files = new ArrayList<>();
		final URL resource = this.getResource(manifest);
		if (resource == null) {
			return files;
		}

		try (final InputStream is = resource.openStream()) {
			return CharStreams.readLines(new InputStreamReader(is), new LineProcessor<List<String>>() {
				@Override
				public boolean processLine(String line) throws IOException {
					if (line.startsWith("//= ")) {
						line = line.substring(3).trim();
						files.add(line);
					}
					return true;
				}
				@Override
				public List<String> getResult() {
					return files;
				}
			});
		}
	}

	@Override
	protected List<String> getScripts() throws IOException {
		return files(getSrc());
	}

	@Override
	protected void doTag(String src) throws IOException {
		final JspWriter writer = getJspContext().getOut();
		writer.println("<script src=\"" + src + "\"></script>");
	}
}
