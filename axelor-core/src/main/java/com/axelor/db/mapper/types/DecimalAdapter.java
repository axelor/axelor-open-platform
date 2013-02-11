package com.axelor.db.mapper.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.validation.constraints.Digits;

import com.axelor.db.mapper.TypeAdapter;

public class DecimalAdapter implements TypeAdapter<BigDecimal> {

	@Override
	public Object adapt(Object value, Class<?> actualType, Type genericType,
			Annotation[] annotations) {
		
		Integer scale = null;
		for (Annotation a : annotations) {
			if (a instanceof Digits) {
				scale = ((Digits) a).fraction();
			}
		}
		
		if (value == null || (value instanceof String && "".equals(((String) value).trim())))
			return BigDecimal.ZERO;

		if (value instanceof BigDecimal)
			return adjust((BigDecimal)value, scale);
		
		return adjust(new BigDecimal(value.toString()), scale);
	}
	
	private BigDecimal adjust(BigDecimal value, Integer scale) {
		if (scale != null)
			return value.setScale(scale, RoundingMode.HALF_UP);
		return value;
	}
}
