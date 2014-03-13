/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
