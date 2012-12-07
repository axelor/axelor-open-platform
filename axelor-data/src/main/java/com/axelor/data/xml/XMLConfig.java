package com.axelor.data.xml;

import java.io.File;
import java.util.List;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("xml-inputs")
public class XMLConfig {

	@XStreamImplicit(itemFieldName = "adapter")
	private List<XMLAdapter> adapters = Lists.newArrayList();
	
	@XStreamImplicit(itemFieldName = "input")
	private List<XMLInput> inputs = Lists.newArrayList();

	public List<XMLAdapter> getAdapters() {
		if (adapters == null) {
			adapters = Lists.newArrayList();
		}
		return adapters;
	}

	public List<XMLInput> getInputs() {
		return inputs;
	}
	
	public static XMLConfig parse(File input) {
		XStream stream = new XStream();
		stream.setMode(XStream.NO_REFERENCES);
		stream.processAnnotations(XMLConfig.class);
		return (XMLConfig) stream.fromXML(input);
	}
}
