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
	 * 
	 * @param <T>
	 *            type of the context bean
	 * @param context
	 *            the template context
	 * 
	 * @return a {@link Renderer} instance
	 */
	<T extends Model> Renderer make(T context);
}
