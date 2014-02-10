package com.axelor.meta.domains;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class Sequence {

	@XmlAttribute
	private String name;
	
	@XmlAttribute
	private String prefix;
	
	@XmlAttribute
	private String sufix;
	
	@XmlAttribute
	private Integer padding;
	
	@XmlAttribute
	private Long initial;
	
	@XmlAttribute
	private Integer increment;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getSufix() {
		return sufix;
	}

	public void setSufix(String sufix) {
		this.sufix = sufix;
	}

	public Integer getPadding() {
		return padding;
	}

	public void setPadding(Integer padding) {
		this.padding = padding;
	}

	public Long getInitial() {
		return initial;
	}

	public void setInitial(Long initial) {
		this.initial = initial;
	}

	public Integer getIncrement() {
		return increment;
	}

	public void setIncrement(Integer increment) {
		this.increment = increment;
	}
}
