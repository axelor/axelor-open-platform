/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.tools.x2j.pojo

import java.util.List

import com.axelor.common.StringUtils
import com.axelor.tools.x2j.Utils

import groovy.util.slurpersupport.NodeChild

class EnumItem {

	EnumType entity

	String name

	String value
	
	String title
	
	String help

	EnumItem(EnumType entity, NodeChild node) {
		this.entity = entity
		this.name = node.@name
		this.value = node.@value
		this.title = node.@title
		this.help = node.@help
	}
	
	String getDocumentation() {
		String text = Utils.stripCode(help, "\n * ")
		if (text == "") {
			return ""
		}
		return """
\t/**
\t * """ + text + """
\t */"""
	}

	private String quote(String text) {
		if (text) {
			return '"' + text + '"';
		}
		return null;
	}

	private Annotation $widget() {
		if (title) {
			return new Annotation(entity.importManager, "com.axelor.db.annotations.Widget", false)
				.add("title", title);
		}
		return null
	}

	String getItemCode() {
		def args = []
		if (value) {
			args = [entity.numeric ? value : quote(value)]
		}
		String code = name
		if (args.size() > 0) {
			code += "(" + String.join(", ", args) + ")"
		}
		def annon = $widget()
		if (annon != null) {
			code = annon.toString() + "\n\t" + code;
		}

		return """
${documentation}
	${code}"""
	}
}
