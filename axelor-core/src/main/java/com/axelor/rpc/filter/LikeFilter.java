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
package com.axelor.rpc.filter;

import com.axelor.db.internal.DBHelper;

class LikeFilter extends SimpleFilter {

	private LikeFilter(Operator operator, String fieldName, String value) {
		super(operator, fieldName, value);
	}

	private static String format(Object value) {
		String text = value.toString().toUpperCase();
		if (text.matches("(^%.*)|(.*%$)")) {
			return text;
		}
		return text = "%" + text + "%";
	}
	
	public static LikeFilter like(String fieldName, Object value) {
		return new LikeFilter(Operator.LIKE, fieldName, format(value));
	}
	
	public static LikeFilter notLike(String fieldName, Object value) {
		return new LikeFilter(Operator.NOT_LIKE, fieldName, format(value));
	}

	@Override
	public String getQuery() {
		if (DBHelper.isUnaccentEnabled()) {
			return String.format("(unaccent(UPPER(self.%s)) %s unaccent(?))", getFieldName(), getOperator());
		}
		return String.format("(UPPER(self.%s) %s ?)", getFieldName(), getOperator());
	}

}
