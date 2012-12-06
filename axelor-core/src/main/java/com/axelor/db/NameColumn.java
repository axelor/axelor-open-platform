package com.axelor.db;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to declare a column as name column that can be
 * used to give name to the record.
 * 
 * This column can be used by UI to display the record with text value of this
 * column.
 * 
 */
@Documented
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface NameColumn {
}
