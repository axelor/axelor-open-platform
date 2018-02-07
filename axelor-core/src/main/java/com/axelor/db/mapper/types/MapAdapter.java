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
package com.axelor.db.mapper.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.TypeAdapter;

public class MapAdapter implements TypeAdapter<Map<?, ?>> {

	static boolean isModelMap(Class<?> type, Object value) {
		return value instanceof Map && Model.class.isAssignableFrom(type) && !Model.class.isInstance(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object adapt(Object value, Class<?> type, Type genericType, Annotation[] annotations) {
		if (isModelMap(type, value)) {
			return Mapper.toBean(type.asSubclass(Model.class), (Map<String, Object>) value);
		}
		return value;
	}
}
