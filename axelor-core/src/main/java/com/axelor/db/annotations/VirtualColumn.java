/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should be used to mark computed fields.
 *
 * <p>For example:
 *
 * <pre>
 * &#064;Entity
 * public class Contact extends Model {
 * 	...
 * 	private String firstName;
 * 	private String lastName;
 * 	...
 * 	...
 * 	{@literal @}VirtualColumn
 * 	public String getFullName() {
 * 		return firstName + " " + lastName;
 * 	}
 * 	...
 * }
 * </pre>
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface VirtualColumn {}
