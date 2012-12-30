package com.axelor.data.csv;

import java.io.File;
import java.util.List;

import com.axelor.data.adapter.DataAdapter;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("csv-inputs")
public class CSVConfig {

	@XStreamImplicit(itemFieldName = "input")
	private List<CSVInput> inputs = Lists.newArrayList();
	
	@XStreamImplicit(itemFieldName = "adapter")
	private List<DataAdapter> adapters = Lists.newArrayList();

	public List<CSVInput> getInputs() {
		return inputs;
	}
	
	public List<DataAdapter> getAdapters() {
		if (adapters == null) {
			adapters = Lists.newArrayList();
		}
		return adapters;
	}

	public static CSVConfig parse(File input) {
		XStream stream = new XStream();
		stream.processAnnotations(CSVConfig.class);
		return (CSVConfig) stream.fromXML(input);
	}
}
