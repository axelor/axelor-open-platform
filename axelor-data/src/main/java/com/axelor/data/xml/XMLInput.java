package com.axelor.data.xml;

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("input")
public class XMLInput {
	
	@XStreamAlias("file")
	@XStreamAsAttribute
	private String fileName;
	
	@XStreamAsAttribute
	private String root;

	@XStreamImplicit(itemFieldName = "adapter")
	private List<XMLAdapter> adapters = Lists.newArrayList();
	
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
	
	public List<XMLAdapter> getAdapters() {
		if (adapters == null) {
			adapters = Lists.newArrayList();
		}
		return adapters;
	}
	
	public List<XMLBind> getBindings() {
		return bindings;
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("file", fileName)
				.add("bindings", bindings).toString();
	}
}
