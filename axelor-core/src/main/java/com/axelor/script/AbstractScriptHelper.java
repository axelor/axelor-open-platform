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