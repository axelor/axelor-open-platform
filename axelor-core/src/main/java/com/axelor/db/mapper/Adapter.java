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
package com.axelor.db.mapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axelor.db.mapper.types.DecimalAdapter;
import com.axelor.db.mapper.types.JavaTimeAdapter;
import com.axelor.db.mapper.types.ListAdapter;
import com.axelor.db.mapper.types.MapAdapter;
import com.axelor.db.mapper.types.SetAdapter;
import com.axelor.db.mapper.types.SimpleAdapter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class Adapter {

	private static SimpleAdapter simpleAdapter = new SimpleAdapter();
	private static ListAdapter listAdapter = new ListAdapter();
	private static SetAdapter setAdapter = new SetAdapter();
	private static MapAdapter mapAdapter = new MapAdapter();
	private static JavaTimeAdapter javaTimeAdapter = new JavaTimeAdapter();
	
	private static DecimalAdapter decimalAdapter = new DecimalAdapter();

	public static Object adapt(Object value, Class<?> type, Type genericType, Annotation[] annotations) {

		if (annotations == null) {
			annotations = new Annotation[]{};
		}

		// one2many
		if (value instanceof Collection && List.class.isAssignableFrom(type)) {
			return listAdapter.adapt(value, type, genericType, annotations);
		}

		// many2many
		if (value instanceof Collection && Set.class.isAssignableFrom(type)) {
			return setAdapter.adapt(value, type, genericType, annotations);
		}

		// many2one
		if (value instanceof Map) {
			return mapAdapter.adapt(value, type, genericType, annotations);
		}

		if (type.isInstance(value)) {
			return value;
		}

		// collection of simple types
		if (value instanceof Collection) {
			Collection<Object> all = value instanceof Set ? Sets.newHashSet() : Lists.newArrayList();
			for (Object item : (Collection<?>) value) {
				all.add(adapt(item, type, genericType, annotations));
			}
			return all;
		}
		
		if (javaTimeAdapter.isJavaTimeObject(type)) {
			return javaTimeAdapter.adapt(value, type, genericType, annotations);
		}

		if (BigDecimal.class.isAssignableFrom(type)) {
			return decimalAdapter.adapt(value, type, genericType, annotations);
		}

		return simpleAdapter.adapt(value, type, genericType, annotations);
	}
}
