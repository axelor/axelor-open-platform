package com.axelor.text;

/**
 * This interface defines API for template engine integration.
 * 
 */
public interface Templates {

	/**
	 * Create a new {@link Template} instance from the given template text.
	 * 
	 * @param text
	 *            the template text
	 * @return an instance of {@link Template}
	 */
	Template fromText(String text);
}
