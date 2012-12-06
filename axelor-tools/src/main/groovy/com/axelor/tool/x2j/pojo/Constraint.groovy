package com.axelor.tool.x2j.pojo

import groovy.util.slurpersupport.NodeChild

class Constraint {

	String name

	String columns
	
	Entity entity

	Constraint(Entity entity, NodeChild node) {
		this.entity = entity
		this.name = node.@name
		this.columns = node.@columns
		
		if (!this.columns) {
			throw new IllegalArgumentException("Column names not specified.")
		}
	}

	List<String> getColumns() {
		if (this.columns == null || this.columns.empty)
			return []
		return this.columns.split(/,/) as List
	}
}