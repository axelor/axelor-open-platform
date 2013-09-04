/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.meta.schema.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.joda.time.DateTime;

import com.axelor.meta.ActionHandler;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

@XmlType
public class ActionExport extends Action {
	
	private static final String DEFAULT_OUTPUT = "/tmp/data-export/${date}/${name}";

	@XmlAttribute(name = "output")
	private String output;
	
	@XmlElement(name = "export")
	private List<Export> exports;

	public String getOutput() {
		return output;
	}
	
	public List<Export> getExports() {
		return exports;
	}
	
	protected void doExport(String dir, Export export, ActionHandler handler) throws IOException {
		export.template = handler.evaluate(export.template).toString();

		File template = new File(export.template);
		if (!template.isFile()) {
			throw new FileNotFoundException("No such template: " + export.template);
		}

		String name = export.getName();
		if (name.indexOf("$") > -1) {
			name = handler.evaluate("eval: \"\"\"" + name + "\"\"\"").toString();
		}

		log.info("export {} as {}", export.getTemplate(), name);
		
		File output = new File(Files.simplifyPath(dir + "/" + name));
		String contents = handler.template(template);
		
		Files.createParentDirs(output);
		Files.write(contents, output, Charsets.UTF_8);
		
		log.info("file saved: {}", output);
	}

	@Override
	public Object evaluate(ActionHandler handler) {
		log.info("action-export: {}", getName());

		String dir = output == null ? DEFAULT_OUTPUT : output;

		dir = dir.replace("${name}", getName())
				 .replace("${date}", new DateTime().toString("yyyyMMdd"))
				 .replace("${time}", new DateTime().toString("HHmmss"));
		dir = handler.evaluate(dir).toString();
		
		for(Export export : exports) {
			if(!export.test(handler)){
				continue;
			}
			try {
				doExport(dir, export, handler);
				return ImmutableMap.of("flash", "Export terminé.");
			} catch (Exception e) {
				log.error("error while exporting: ", e);
				return ImmutableMap.of("error", e.getMessage());
			}
		}
		return null;
	}

	public Object wrap(ActionHandler handler) {
		return evaluate(handler);
	}

	@XmlType
	public static class Export extends Element {
		
		@XmlAttribute
		private String template;
		
		public String getTemplate() {
			return template;
		}
		
		@Override
		public String toString() {
			return Objects.toStringHelper(getClass())
					.add("name", getName())
					.add("template", template)
					.toString();
		}
	}
}
