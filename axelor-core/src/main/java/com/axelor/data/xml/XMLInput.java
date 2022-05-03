/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.data.xml;

import com.axelor.data.adapter.DataAdapter;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.util.List;

@XStreamAlias("input")
public class XMLInput {

  @XStreamAlias("file")
  @XStreamAsAttribute
  private String fileName;

  @XStreamAsAttribute private String root;

  @XStreamImplicit(itemFieldName = "adapter")
  private List<DataAdapter> adapters = Lists.newArrayList();

  @XStreamImplicit(itemFieldName = "bind")
  private List<XMLBind> bindings = Lists.newArrayList();

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
      adapters = Lists.newArrayList();
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
