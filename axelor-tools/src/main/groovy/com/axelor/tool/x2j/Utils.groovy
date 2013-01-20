package com.axelor.tool.x2j

class Utils {

	/**
	 * Strip the extra leading white space from the code string.
	 * 
	 */
	public static String stripCode(String code, String joinWith) {
		if (code == null || code.trim().length() == 0) {
			return ""
		}
		String text = code.stripIndent().replaceAll("    ", "\t")
		text = text.trim().replaceAll("\n", joinWith).trim()
		return text
	}
}
