/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.data.xml;

import com.axelor.data.adapter.DataAdapter;
import com.google.common.base.MoreObjects;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.util.ArrayList;
import java.util.List;

@XStreamAlias("input")
public class XMLInput {

  @XStreamAlias("file")
  @XStreamAsAttribute
  private String fileName;

  @XStreamAsAttribute private String root;

  @XStreamImplicit(itemFieldName = "adapter")
  private List<DataAdapter> adapters = new ArrayList<>();

  @XStreamImplicit(itemFieldName = "bind")
  private List<XMLBind> bindings = new ArrayList<>();

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getRoot() {
    return root;
  }

  public void setRoot(String root) {
    this.root = root;
  }

  public List<DataAdapter> getAdapters() {
    if (adapters == null) {
      adapters = new ArrayList<>();
    }
    return adapters;
  }

  public void setAdapters(List<DataAdapter> adapters) {
    this.adapters = adapters;
  }

  public List<XMLBind> getBindings() {
    return bindings;
  }

  public void setBindings(List<XMLBind> bindings) {
    this.bindings = bindings;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("file", fileName)
        .add("bindings", bindings)
        .toString();
  }
}
