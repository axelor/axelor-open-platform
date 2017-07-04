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
package com.axelor.db.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to specify custom change tracking messages.
 *
 */
@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackMessage {

	/**
	 * The track message to generate if the given condition is true.
	 *
	 * @return the message
	 */
	String message();

	/**
	 * The condition to check.
	 *
	 * @return the condition
	 */
	String condition();

	/**
	 * Specify the events on which to use this message.
	 * 
	 * @return the events
	 */
	TrackEvent[] on() default TrackEvent.ALWAYS;

	/**
	 * Provide tag style if this message is a tag.
	 *
	 * <ul>
	 *   <li>success</li>
	 *   <li>warning</li>
	 *   <li>important</li>
	 *   <li>info</li>
	 * </ul>
	 *
	 * @return the tag style name
	 */
	String tag() default "";

	/**
	 * Only use the message if these fields are changed.
	 * 
	 * @return the field names
	 */
	String[] fields() default "";
}
