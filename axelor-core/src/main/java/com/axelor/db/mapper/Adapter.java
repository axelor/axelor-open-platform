/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
import com.axelor.db.mapper.types.JodaAdapter;
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
	private static JodaAdapter jodaAdapter = new JodaAdapter();
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

		if (jodaAdapter.isJodaObject(type)) {
			return jodaAdapter.adapt(value, type, genericType, annotations);
		}

		if (BigDecimal.class.isAssignableFrom(type)) {
			return decimalAdapter.adapt(value, type, genericType, annotations);
		}

		return simpleAdapter.adapt(value, type, genericType, annotations);
	}
}
