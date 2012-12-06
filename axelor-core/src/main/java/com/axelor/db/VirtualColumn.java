package com.axelor.db;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should be used to mark computed fields.
 * 
 * For example:
 * 
 * <pre>
 * &#064;Entity
 * public class Contact extends Model {
 * 	...
 * 	private String firstName;
 * 	private String lastName;
 * 	...
 * 	...
 * 	@VirtualColumn
 * 	public String getFullName() {
 * 		return firstName + " " + lastName;
 * 	}
 * 	...
 * }
 * </pre>
 * 
 */
@Documented
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface VirtualColumn {

}
