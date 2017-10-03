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
package com.axelor.shell.core;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class ParserResult {

	private final Method method;
	
	private final Object instance;
	
	private final Object[] arguments;
	
	public ParserResult(Method method, Object instance) {
		this(method, instance, null);
	}
	
	public ParserResult(Method method, Object instance, Object[] arguments) {
		this.method = method;
		this.instance = instance;
		this.arguments = arguments;
	}
	
	public Method getMethod() {
		return method;
	}
	
	public Object getInstance() {
		return instance;
	}
	
	public Object[] getArguments() {
		return arguments;
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(31, method, instance, arguments);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		if (!Arrays.equals(arguments, ((ParserResult) obj).arguments)) return false;
		if (!Objects.equal(instance, ((ParserResult) obj).instance)) return false;
		if (!Objects.equal(method, ((ParserResult) obj).method)) return false;
		return true;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("method", method)
				.add("instance", instance)
				.add("arguments", arguments != null ? Joiner.on(", ").join(arguments) : "[]")
				.toString();
	}
}
