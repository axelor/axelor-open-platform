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
package com.axelor.data.csv;

import java.io.File;
import java.util.List;

import com.axelor.common.VersionUtils;
import com.axelor.data.adapter.DataAdapter;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("csv-inputs")
public class CSVConfig {

	public static final String NAMESPACE = "http://axelor.com/xml/ns/data-import";

	public static final String VERSION = VersionUtils.getVersion().feature;

	@XStreamImplicit(itemFieldName = "input")
	private List<CSVInput> inputs = Lists.newArrayList();

	@XStreamImplicit(itemFieldName = "adapter")
	private List<DataAdapter> adapters = Lists.newArrayList();

	/**
	 * Get all {@link #inputs} nodes
	 * 
	 * @return the inputs
	 */
	public List<CSVInput> getInputs() {
		return inputs;
	}

	/**
	 * Set {@link #inputs} nodes
	 * 
	 * @param inputs the inputs
	 */
	public void setInputs(List<CSVInput> inputs) {
		this.inputs = inputs;
	}

	/**
	 * Get all {@link #adapters} nodes.
	 * 
	 * @return list of all adapters
	 */
	public List<DataAdapter> getAdapters() {
		if (adapters == null) {
			adapters = Lists.newArrayList();
		}
		return adapters;
	}

	/**
	 * Parse the <code>input</code> File
	 * 
	 * @param input
	 *            the input file
	 * @return an instance of {@link CSVConfig} for the given file
	 */
	public static CSVConfig parse(File input) {
		XStream stream = new XStream();
		stream.processAnnotations(CSVConfig.class);
		return (CSVConfig) stream.fromXML(input);
	}
}
