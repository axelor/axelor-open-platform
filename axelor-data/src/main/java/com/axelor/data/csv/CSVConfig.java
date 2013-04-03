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

	/**
	 * Get all {@link #inputs} nodes
	 * @return List<CSVInput>
	 */
	public List<CSVInput> getInputs() {
		return inputs;
	}
	
	/**
	 * Set {@link #inputs} nodes
	 * @param inputs
	 */
	public void setInputs(List<CSVInput> inputs) {
		this.inputs = inputs;
	}
	
	/**
	 * Get all {@link #adapters} nodes.
	 * If {@link #adapters} is null, return a new list of {@see DataAdapter}.
	 * @return
	 */
	public List<DataAdapter> getAdapters() {
		if (adapters == null) {
			adapters = Lists.newArrayList();
		}
		return adapters;
	}

	/**
	 * Parse the <code>input</code> File
	 * @param input File
	 * @return
	 */
	public static CSVConfig parse(File input) {
		XStream stream = new XStream();
		stream.processAnnotations(CSVConfig.class);
		return (CSVConfig) stream.fromXML(input);
	}
}
