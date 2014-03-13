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

import groovy.text.GStringTemplateEngine;
import groovy.text.TemplateEngine;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.codehaus.groovy.control.CompilationFailedException;

import com.axelor.db.Model;
import com.axelor.rpc.Context;
import com.axelor.script.ScriptBindings;
import com.google.common.base.Throwables;

/**
 * The implementation of {@link Templates} for groovy string template support.
 * 
 */
public class GroovyTemplates implements Templates {

	private static final TemplateEngine engine = new GStringTemplateEngine();

	class GroovyTemplate implements Template {

		private groovy.text.Template template;

		public GroovyTemplate(groovy.text.Template template) {
			this.template = template;
		}

		@Override
		public Renderer make(final Map<String, Object> context) {
			final ScriptBindings bindings = new ScriptBindings(context);
			return new Renderer() {
				@Override
				public void render(Writer out) throws IOException {
					template.make(bindings).writeTo(out);
				}
			};
		}

		@Override
		public <T extends Model> Renderer make(T context) {
			return make(Context.create(context));
		}
	}

	@Override
	public Template fromText(String text) {
		try {
			return new GroovyTemplate(engine.createTemplate(text));
		} catch (CompilationFailedException | ClassNotFoundException
				| IOException e) {
			throw Throwables.propagate(e);
		}
	}
}
