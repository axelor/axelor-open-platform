/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.data.csv;

import com.axelor.common.VersionUtils;
import com.axelor.data.XStreamUtils;
import com.axelor.data.adapter.DataAdapter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@XStreamAlias("csv-inputs")
public class CSVConfig {

  public static final String NAMESPACE = "http://axelor.com/xml/ns/data-import";

  public static final String VERSION = VersionUtils.getVersion().feature;

  @XStreamImplicit(itemFieldName = "input")
  private List<CSVInput> inputs = new ArrayList<>();

  @XStreamImplicit(itemFieldName = "adapter")
  private List<DataAdapter> adapters = new ArrayList<>();

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
      adapters = new ArrayList<>();
    }
    return adapters;
  }

  /**
   * Parse the <code>input</code> File
   *
   * @param input the input file
   * @return an instance of {@link CSVConfig} for the given file
   */
  public static CSVConfig parse(File input) {
    XStream stream = XStreamUtils.createXStream();
    stream.processAnnotations(CSVConfig.class);
    stream.registerConverter(
        new CSVInputConverter(
            stream.getConverterLookup().lookupConverterForType(CSVInput.class),
            stream.getReflectionProvider()));
    stream.registerConverter(
        new CSVBindConverter(
            stream.getConverterLookup().lookupConverterForType(CSVBind.class),
            stream.getReflectionProvider()));
    return (CSVConfig) stream.fromXML(input);
  }
}
