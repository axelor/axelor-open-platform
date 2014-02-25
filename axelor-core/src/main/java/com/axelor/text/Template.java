package com.axelor.text;

import java.util.Map;

import com.axelor.db.Model;

/**
 * The {@link Template} interface defines methods to render the template.
 * 
 */
public interface Template {

	/**
	 * Make a template renderer using the given context.
	 * 
	 * @param context
	 *            the template context
	 * @return a {@link Renderer} instance
	 */
	Renderer make(Map<String, Object> context);

	/**
	 * Make a template renderer using the given context.
	 * @param context the template context
	 * @return a {@link Renderer} instance
	 */
	<T extends Model> Renderer make(T context);
}
