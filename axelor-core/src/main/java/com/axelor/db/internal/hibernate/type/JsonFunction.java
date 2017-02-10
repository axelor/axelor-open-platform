/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.db.internal.hibernate.type;

import com.google.common.base.Preconditions;

public final class JsonFunction {

	public static final String DEFAULT_TYPE = "text";

	private String field;

	private String attribute;

	private String type;

	public JsonFunction(String field, String attribute, String type) {
		this.field = field;
		this.attribute = attribute;
		this.type = type;
	}

	public JsonFunction(String field, String attribute) {
		this(field, attribute, DEFAULT_TYPE);
	}

	public static JsonFunction fromPath(String path) {
		Preconditions.checkArgument(path != null, "name cannot be null");
		Preconditions.checkArgument(path.indexOf('.') > -1, "not a json path");

		final int dot = path.indexOf('.');
		final int col = path.indexOf("::");

		final String type = col == -1 ? DEFAULT_TYPE : path.substring(col + 2);
		final String rest = col == -1 ? path : path.substring(0, col);
		final String field = rest.substring(0, dot);
		final String attribute = rest.substring(dot + 1);

		return new JsonFunction(field, attribute, type);
	}

	public String getField() {
		return field;
	}

	public String getAttribute() {
		return attribute;
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder()
				.append("json_extract_").append(type).append("(")
				.append("self.").append(field);
		for (String item : attribute.split("\\.")) {
			builder.append(", ").append("'").append(item).append("'");
		}
		return builder.append(")").toString();
	}
}
