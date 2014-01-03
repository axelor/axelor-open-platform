/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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

	public static final String NAMESPACE = "http://apps.axelor.com/xml/ns/data-import";

	public static final String VERSION = "0.9";

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
