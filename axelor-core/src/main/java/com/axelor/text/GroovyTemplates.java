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
