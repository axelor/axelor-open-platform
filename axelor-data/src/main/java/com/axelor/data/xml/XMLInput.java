package com.axelor.data.xml;

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("input")
public class XMLInput {
	
	@Inject
	Injector injector;

	@XStreamAlias("file")
	@XStreamAsAttribute
	private String fileName;
	
	@XStreamAsAttribute
	private String root;

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
