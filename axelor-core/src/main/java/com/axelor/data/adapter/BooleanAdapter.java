/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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
package com.axelor.data.adapter;

import java.util.Map;
import java.util.regex.Pattern;

public class BooleanAdapter extends Adapter {

	private Pattern pattern;

	@Override
	public Object adapt(Object value, Map<String, Object> context) {
		
		if (value == null || "".equals(value)) {
			return Boolean.FALSE;
		}
		
		if (pattern == null) {
			String falsePattern = this.get("falsePattern", "(0|false|no|f|n)");
			pattern = Pattern.compile(falsePattern, Pattern.CASE_INSENSITIVE);
		}

		return !pattern.matcher((String) value).matches();
	}
}
