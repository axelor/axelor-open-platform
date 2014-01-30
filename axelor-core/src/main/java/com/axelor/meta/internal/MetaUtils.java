package com.axelor.meta.internal;

import java.io.File;
import java.util.regex.Pattern;

public final class MetaUtils {

	private static final String UNX_PATTERN = "(/WEB-INF/lib/%s-)|(%s/WEB-INF/classes/)";
	
	private static final String WIN_PATTERN = "(\\\\WEB-INF\\\\lib\\\\%s-)|(%s\\\\WEB-INF\\\\classes\\\\)";

	private static String moduleNamePattern = "\\".equals(File.separator) ? WIN_PATTERN : UNX_PATTERN;

	public static Pattern getModuleNamePattern(String module) {
		return Pattern.compile(String.format(moduleNamePattern, module, module));
	}
}
