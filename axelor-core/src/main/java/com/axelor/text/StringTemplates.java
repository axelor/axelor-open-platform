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
package com.axelor.text;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.stringtemplate.v4.NoIndentWriter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import com.axelor.db.Model;
import com.axelor.rpc.Context;
import com.axelor.script.ScriptBindings;

/**
 * The implementation of {@link Templates} for the StringTemplate (ST4) support.
 * 
 */
public class StringTemplates implements Templates {

	class StringTemplate implements Template {

		private ST template;

		private StringTemplate(ST template) {
			this.template = template;
		}

		@Override
		public Renderer make(Map<String, Object> context) {
			Map<String, Object> vars = new ScriptBindings(context);

			for (String key : vars.keySet()) {
				try {
					template.add(key, vars.get(key));
				} catch (Exception e) {
				}
			}

			return new Renderer() {
				@Override
				public void render(Writer out) throws IOException {
					try {
						template.write(new NoIndentWriter(out));
					} catch (IOException e) {
					}
				}
			};
		}

		@Override
		public <T extends Model> Renderer make(T context) {
			return make(Context.create(context));
		}
	}

	private static final char DEFAULT_START_DELIMITER = '<';
	private static final char DEFAULT_STOP_DELIMITER = '>';

	private final STGroup group;

	public StringTemplates() {
		this(DEFAULT_START_DELIMITER, DEFAULT_STOP_DELIMITER);
	}

	public StringTemplates(char delimiterStartChar, char delimiterStopChar) {
		this.group = new STGroup(delimiterStartChar, delimiterStopChar);
	}

	@Override
	public Template fromText(String text) {
		ST template = new ST(group, text);
		return new StringTemplate(template);
	}
}
