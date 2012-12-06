package com.axelor.meta.views;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("notebook")
public class Notebook extends AbstractContainer {

	@XmlElements({
		@XmlElement(name = "page", type = Page.class, required = true)
	})
	private List<Page> pages;
	
	public List<Page> getPages() {
		return pages;
	}
	
	public void setPages(List<Page> pages) {
		this.pages = pages;
	}
}
