package com.axelor.data.csv;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("csv-inputs")
public class CSVConfig {

	@XStreamImplicit(itemFieldName = "input")
	private List<CSVInput> inputs = Lists.newArrayList();

	public List<CSVInput> getInputs() {
		return inputs;
	}

	public static CSVConfig parse(File input) throws IOException {
		XStream stream = new XStream();
		stream.processAnnotations(CSVConfig.class);
		return (CSVConfig) stream.fromXML(input);
	}
}
