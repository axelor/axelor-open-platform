/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.data.xml;

import com.axelor.data.XStreamUtils;
import com.axelor.data.adapter.DataAdapter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@XStreamAlias("xml-inputs")
public class XMLConfig {

  @XStreamImplicit(itemFieldName = "adapter")
  private List<DataAdapter> adapters = new ArrayList<>();

  @XStreamImplicit(itemFieldName = "input")
  private List<XMLInput> inputs = new ArrayList<>();

  public List<DataAdapter> getAdapters() {
    if (adapters == null) {
      adapters = new ArrayList<>();
    }
    return adapters;
  }

  public List<XMLInput> getInputs() {
    return inputs;
  }

  public static XMLConfig parse(File input) {
    XStream stream = XStreamUtils.createXStream();
    stream.setMode(XStream.NO_REFERENCES);
    stream.processAnnotations(XMLConfig.class);
    stream.registerConverter(
        new XMLBindConverter(
            stream.getConverterLookup().lookupConverterForType(XMLBind.class),
            stream.getReflectionProvider()));
    return (XMLConfig) stream.fromXML(input);
  }
}
