/**
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
package com.axelor.shell.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a class method that provides a command to the shell.
 * 
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CliCommand {

	/**
	 * @return name of the command.
	 */
	String name();
	
	/**
	 * @return additional text followed by name to show on usage message.
	 */
	String usage() default "";

	/**
	 * @return a help message for this command, shown as a header of usage info.
	 */
	String help() default "";

	/**
	 * @return some notes for this commands, shown as a footer of usage info.
	 */
	String notes() default "";
	
	/**
	 * @return command sort order priority.
	 */
	int priority() default -1;
	
	/**
	 * @return whether to show this command in usage help.
	 */
	boolean hidden() default false;
}
