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
package com.axelor.meta.schema.actions;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.eclipse.persistence.oxm.annotations.XmlCDATA;

import com.axelor.meta.ActionHandler;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.script.GroovyScriptHelper;
import com.axelor.script.NashornScriptHelper;
import com.axelor.script.ScriptHelper;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ActionScript extends Action {

	private static final String LANGUAGE_JS = "js";

	private static final String KEY_REQUEST = "$request";
	private static final String KEY_RESPONSE = "$response";

	@JsonIgnore
	@XmlElement(name = "script")
	private ActScript script;

	public ActScript getScript() {
		return script;
	}

	public void setScript(ActScript script) {
		this.script = script;
	}

	private ScriptHelper getScriptHelper(Bindings context) {
		return LANGUAGE_JS.equalsIgnoreCase(script.language)
				? new NashornScriptHelper(context)
				: new GroovyScriptHelper(context);
	}

	@Override
	public Object evaluate(ActionHandler handler) {
		final Bindings context = new SimpleBindings();
		final ActionRequest request = handler.getRequest();
		final ActionResponse response = new ActionResponse();
		context.put(KEY_REQUEST, request);
		context.put(KEY_RESPONSE, response);
		try {
			getScriptHelper(context).eval(script.code.trim());
		} finally {
		}
		return response;
	}

	@Override
	public Object wrap(ActionHandler handler) {
		return evaluate(handler);
	}

	@XmlType
	public static class ActScript {

		@XmlAttribute
		private String language;

		@XmlCDATA
		@XmlValue
		public String code;

		public String getLanguage() {
			return language;
		}

		public void setLanguage(String language) {
			this.language = language;
		}
	}
}
