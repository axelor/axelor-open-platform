/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.tools.x2j.pojo

class Annotation {

	String name

	List<String> args = []

	boolean empty

	Entity entity

	Annotation(Entity entity, String name) {
		this(entity, name, false)
	}

	Annotation(Entity entity, String name, boolean empty) {
		this.entity = entity
		this.name = "@" + entity.importType(name)
		this.empty = empty
	}

	boolean isEmpty() {
		args.empty && !empty
	}

	String quote(String s) {
		if (!s?.startsWith('"'))
			return '"' + s + '"'
		return s
	}

	String wrap(List<String> names) {
		def value = names.join(", ")
		entity.groovy ? "[ $value ]" : "{ $value }"
	}

	Annotation add(String value) {
		this.add(value, true)
	}

	Annotation add(String value, boolean quote) {
		if (value != null)
			args.add(quote ? this.quote(value) : value)
		this
	}

	Annotation add(String param, String value) {
		this.add(param, value, true)
	}

	Annotation add(String param, String value, boolean quote) {
		if (value == null)
			return this
		this.add(param, [ value ], quote, true)
	}

	Annotation add(String param, String value, boolean quote, boolean array) {
		if (value == null)
			return this
		def values = array ? value.split(/,/) as List : [ value ]
		this.add(param, values, quote, ! array)
	}

	Annotation add(String param, List<?> values, boolean quote) {
		this.add(param, values, quote, false)
	}

	Annotation add(String param, List<?> values, boolean quote, boolean unwrapSingle) {
		if (values == null)
			return this;

		values = values.collect {
			if (it instanceof Annotation)
				return it
			quote ? this.quote(it) : entity.importType(it)
		}

		def value = unwrapSingle && values.size() == 1 ? values[0] : wrap(values)

		args.add("$param = $value")
		return this
	}

	@Override
	String toString() {
		if (args.empty)
			return name
		"${name}(${args.join(', ')})"
	}
}
