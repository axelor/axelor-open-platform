package com.axelor.script;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.axelor.common.StringUtils;
import com.google.common.base.Preconditions;

public abstract class AbstractScriptHelper implements ScriptHelper {

	private ScriptBindings bindings;

	@Override
	public ScriptBindings getBindings() {
		return bindings;
	}

	@Override
	public void setBindings(ScriptBindings bindings) {
		this.bindings = bindings;
	}

	@Override
	public final boolean test(String expr) {
		if (StringUtils.isBlank(expr))
			return true;
		Object result = eval(expr);
		if (result == null)
			return false;
		if (result instanceof Number && result.equals(0))
			return false;
		if (result instanceof Boolean)
			return (Boolean) result;
		return true;
	}

	@Override
	public Object call(Object obj, String methodCall) {
		Preconditions.checkNotNull(obj);
		Preconditions.checkNotNull(methodCall);

		Pattern p = Pattern.compile("(\\w+)\\((.*?)\\)");
		Matcher m = p.matcher(methodCall);

		if (!m.matches()) {
			return null;
		}

		return doCall(obj, methodCall);
	}

	protected abstract Object doCall(Object obj, String methodCall);
}