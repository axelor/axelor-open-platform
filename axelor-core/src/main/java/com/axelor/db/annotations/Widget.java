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
package com.axelor.db.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides information about UI widget representing this field.
 *
 */
@Documented
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Widget {

	/**
	 * Title for the UI widget.
	 *
	 */
	String title() default "";

	/**
	 * Help text.
	 *
	 */
	String help() default "";

	/**
	 * Whether the widget should be set readonly.
	 *
	 */
	boolean readonly() default false;

	/**
	 * Whether to hide widget by default.
	 *
	 */
	boolean hidden() default false;

	/**
	 * Should be used with String fields to mark whether to use multiline text
	 * widget.
	 *
	 */
	boolean multiline() default false;

	/**
	 * Use image widget for this binary field.
	 *
	 */
	boolean image() default false;

	/**
	 * List of the columns to be used to search this record.
	 *
	 * Used by auto-complete widget. By default the same column will be
	 * searched. Also, in case of virtual column (computed values) specify the
	 * actual searchable columns.
	 *
	 */
	String[] search() default {};

	/**
	 * List of selection options.
	 *
	 * The list of value->label options for the selection widget. For example:
	 *
	 * <pre>
	 * 	@Widget( select = "['mr', 'Mr.'], ['mrs', 'Mrs.'], ['miss', 'Miss']" )
	 * </pre>
	 *
	 */
	String selection() default "";
}