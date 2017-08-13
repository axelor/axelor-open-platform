/*
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
package com.axelor.db.mapper.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.axelor.db.ValueEnum;
import com.axelor.db.mapper.TypeAdapter;

public class EnumAdapter implements TypeAdapter<Enum<?>> {

	@Override
	@SuppressWarnings("unchecked")
	public Object adapt(Object value, Class<?> actualType, Type genericType, Annotation[] annotations) {
		if (value == null) {
			return null;
		}
		if (!actualType.isEnum()) {
			throw new IllegalArgumentException("Given type is not enum: " + actualType.getName());
		}
		return ValueEnum.class.isAssignableFrom(actualType)
				? ValueEnum.of(actualType.asSubclass(Enum.class), value)
				: Enum.valueOf(actualType.asSubclass(Enum.class), value.toString());
	}
}
