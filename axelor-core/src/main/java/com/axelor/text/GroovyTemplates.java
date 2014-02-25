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
