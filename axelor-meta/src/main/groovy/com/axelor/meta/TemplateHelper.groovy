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
