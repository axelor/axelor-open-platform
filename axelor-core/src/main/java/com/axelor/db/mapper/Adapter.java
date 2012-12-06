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

public class Adapter {

	private static SimpleAdapter simpleAdapter = new SimpleAdapter();
	private static ListAdapter listAdapter = new ListAdapter();
	private static SetAdapter setAdapter = new SetAdapter();
	private static MapAdapter mapAdapter = new MapAdapter();
	private static JodaAdapter jodaAdapter = new JodaAdapter();
	private static DecimalAdapter decimalAdapter = new DecimalAdapter();
	
	public static Object adapt(Object value, Class<?> type, Type genericType, Annotation[] annotations) {
		
		if (annotations == null)
			annotations = new Annotation[]{};
		
		if (value instanceof Collection && List.class.isAssignableFrom(type))
			return listAdapter.adapt(value, type, genericType, annotations);
		
		if (value instanceof Collection && Set.class.isAssignableFrom(type))
			return setAdapter.adapt(value, type, genericType, annotations);
		
		if (value instanceof Map)
			return mapAdapter.adapt(value, type, genericType, annotations);

		if (type.isInstance(value)) {
			return value;
		}

		if (jodaAdapter.isJodaObject(type))
			return jodaAdapter.adapt(value, type, genericType, annotations);
		
		if (BigDecimal.class.isAssignableFrom(type))
			return decimalAdapter.adapt(value, type, genericType, annotations);
		
		return simpleAdapter.adapt(value, type, genericType, annotations);
	}
}
