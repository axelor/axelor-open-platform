package com.axelor.script;

import com.axelor.common.StringUtils;
import com.axelor.rpc.Context;

public class CompositeScriptHelper extends AbstractScriptHelper {

	private GroovyScriptHelper gsh;
	private ELScriptHelper esh;

	public CompositeScriptHelper(ScriptBindings bindings) {
		this.setBindings(bindings);
	}

	public CompositeScriptHelper(Context context) {
		this(new ScriptBindings(context));
	}

	private ScriptHelper getGSH() {
		if (gsh == null) {
			gsh = new GroovyScriptHelper(this.getBindings());
		}
		return gsh;
	}

	private ScriptHelper getESH() {
		if (esh == null) {
			esh = new ELScriptHelper(this.getBindings());
		}
		return esh;
	}

	private String strip(String expr) {
		String str = expr.trim();
		return str.substring(2, str.length()-1);
	}

	private boolean isEL(String expr) {
		if (StringUtils.isBlank(expr)) {
			return true;
		}
		String str = expr.trim();
		return str.startsWith("#{") && str.endsWith("}");
	}

	@Override
	public Object call(Object obj, String methodCall) {
		// allways use EL
		return getESH().call(obj, methodCall);
	}

	@Override
	protected Object doCall(Object obj, String methodCall) {
		return null;
	}

	@Override
	public Object eval(String expr) {
		if (isEL(expr)) {
			return getESH().eval(strip(expr));
		}
		return getGSH().eval(expr);
	}
}
