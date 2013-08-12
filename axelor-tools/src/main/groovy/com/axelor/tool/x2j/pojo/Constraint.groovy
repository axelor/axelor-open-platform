package com.axelor.tool.x2j.pojo

import groovy.transform.CompileStatic
import groovy.util.slurpersupport.NodeChild

@CompileStatic
class Constraint extends Index {

	Constraint(Entity entity, NodeChild node) {
		super(entity, node)
	}
}