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
package com.axelor.meta

import groovy.text.GStringTemplateEngine
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

class TemplateHelper {
	
	@CompileStatic
	static class Mapping extends HashMap {
		
		Binding binding;
		
		public Mapping(Binding binding) {
			this.binding = binding;
		}
			
		@Override
		public boolean containsKey(Object key) {
			return true;
		}
		
		@Override
		public Object get(Object key) {
			return binding.getVariable(key as String)
		}
	}
	
	@CompileStatic
	static String make(final String template, final Binding binding) {
		def engine = new GStringTemplateEngine()
		def mapping = new Mapping(binding);
		
		return engine.createTemplate(template).make(mapping).toString()
	}

	static String serialize(GPathResult gpath) {
		def ns = gpath.lookupNamespace("")
		if (ns) {
			def doc = {
				mkp.declareNamespace("": ns)
				out << gpath
			}
			return XmlUtil.serialize(new StreamingMarkupBuilder().bind(doc))
		}
		return XmlUtil.serialize(gpath)
	}
}
