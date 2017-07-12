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
package com.axelor.meta.schema.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.app.AppSettings;
import com.axelor.common.FileUtils;
import com.axelor.common.ResourceUtils;
import com.axelor.i18n.I18n;
import com.axelor.meta.ActionHandler;
import com.axelor.text.GroovyTemplates;
import com.axelor.text.StringTemplates;
import com.axelor.text.Templates;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.io.Files;

@XmlType
public class ActionExport extends Action {
	
	private static final String DEFAULT_EXPORT_DIR = "{java.io.tmpdir}/axelor/data-export";
	private static final String DEFAULT_DIR = "${date}/${name}";

	@XmlAttribute(name = "output")
	private String output;

	@XmlAttribute(name = "download")
	private Boolean download;

	@XmlElement(name = "export")
	private List<Export> exports;

	public String getOutput() {
		return output;
	}

	public Boolean getDownload() {
		return download;
	}

	public List<Export> getExports() {
		return exports;
	}
	
	public static File getExportPath() {
		final String path = AppSettings.get().getPath("data.export.dir", DEFAULT_EXPORT_DIR);
		return new File(path);
	}

	protected String doExport(String dir, Export export, ActionHandler handler) throws IOException {
		String templatePath = handler.evaluate(export.template).toString();

		Reader reader = null;
		File template = new File(templatePath);
		if (template.isFile()) {
			reader = new FileReader(template);
		}

		if (reader == null) {
			InputStream is = ResourceUtils.getResourceStream(templatePath);
			if (is == null) {
				throw new FileNotFoundException("No such template: " + templatePath);
			}
			reader = new InputStreamReader(is);
		}

		String name = export.getName();
		if (name.indexOf("$") > -1 || (name.startsWith("#{") && name.endsWith("}"))) {
			name = handler.evaluate(toExpression(name, true)).toString();
		}

		log.info("export {} as {}", templatePath, name);

		Templates engine = new StringTemplates('$', '$');
		if ("groovy".equals(export.engine)) {
			engine = new GroovyTemplates();
		}

		File output = getExportPath();
		output = FileUtils.getFile(output, dir, name);

		String contents = null;
		try {
			contents = handler.template(engine, reader);
		} finally {
			reader.close();
		}

		Files.createParentDirs(output);
		Files.write(contents, output, Charsets.UTF_8);

		log.info("file saved: {}", output);

		return FileUtils.getFile(dir, name).toString();
	}

	@Override
	public Object evaluate(ActionHandler handler) {
		log.info("action-export: {}", getName());

		String dir = output == null ? DEFAULT_DIR : output;

		dir = dir.replace("${name}", getName())
				 .replace("${date}", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
				 .replace("${time}", LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
		dir = handler.evaluate(dir).toString();

		for(Export export : exports) {
			if(!export.test(handler)){
				continue;
			}
			final Map<String, Object> result = new HashMap<>();
			try {
				String file = doExport(dir, export, handler);
				if (getDownload() == Boolean.TRUE) {
					result.put("exportFile", file);
					result.put("notify", I18n.get("Export complete."));
					return result;
				}
				result.put("notify", I18n.get("Export complete."));
				return result;
			} catch (Exception e) {
				log.error("error while exporting: ", e);
				result.put("error", e.getMessage());
				return result;
			}
		}
		return null;
	}

	@Override
	public Object wrap(ActionHandler handler) {
		return evaluate(handler);
	}

	@XmlType
	public static class Export extends Element {

		@XmlAttribute
		private String template;

		@XmlAttribute
		private String engine;

		@XmlAttribute(name = "processor")
		private String processor;

		public String getTemplate() {
			return template;
		}

		public String getEngine() {
			return engine;
		}

		public String getProcessor() {
			return processor;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(getClass())
					.add("name", getName())
					.add("template", template)
					.toString();
		}
	}
}
