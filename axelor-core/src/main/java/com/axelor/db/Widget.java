package com.axelor.db;

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