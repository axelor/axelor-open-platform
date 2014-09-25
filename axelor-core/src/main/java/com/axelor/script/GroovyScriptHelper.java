/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.axelor.auth.db.User;
import com.axelor.db.JpaScanner;
import com.axelor.db.Model;
import com.axelor.internal.cglib.proxy.Callback;
import com.axelor.internal.cglib.proxy.CallbackFilter;
import com.axelor.internal.cglib.proxy.Enhancer;
import com.axelor.internal.cglib.proxy.MethodInterceptor;
import com.axelor.internal.cglib.proxy.MethodProxy;
import com.axelor.internal.cglib.proxy.NoOp;
import com.axelor.rpc.Context;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;

public class GroovyScriptHelper implements ScriptHelper {

	private static final CompilerConfiguration config = new CompilerConfiguration();
	private static final GroovyShell SHELL;

	private static final int DEFAULT_CACHE_SIZE = 500;
	private static final int DEFAULT_CACHE_EXPIRE_TIME = 10;

	private static int cacheSize;
	private static int cacheExpireTime;

	private static final Cache<String, Object> CACHE;

	static {
		config.getOptimizationOptions().put("indy", true);
		config.getOptimizationOptions().put("int", false);

		final ImportCustomizer importCustomizer = new ImportCustomizer();

		importCustomizer.addImport("__repo__", "com.axelor.db.JpaRepository");

		importCustomizer.addImports("org.joda.time.DateTime");
		importCustomizer.addImports("org.joda.time.LocalDateTime");
		importCustomizer.addImports("org.joda.time.LocalDate");
		importCustomizer.addImports("org.joda.time.LocalTime");

		config.addCompilationCustomizers(importCustomizer);

		try {
			cacheSize = Integer.parseInt(System.getProperty("axelor.ScriptCacheSize"));
		} catch (Exception e) {
		}
		try {
			cacheExpireTime = Integer.parseInt(System.getProperty("axelor.ScriptCacheExpireTime"));
		} catch (Exception e) {
		}

		if (cacheSize <= 0) {
			cacheSize = DEFAULT_CACHE_SIZE;
		}
		if (cacheExpireTime <= 0) {
			cacheExpireTime = DEFAULT_CACHE_EXPIRE_TIME;
		}

		CACHE = CacheBuilder.newBuilder()
				.maximumSize(cacheSize)
				.expireAfterAccess(cacheExpireTime, TimeUnit.MINUTES)
				.build();

		SHELL = new GroovyShell(JpaScanner.getClassLoader(), new Binding(), config);
	}

	private static final Delegator DELEGATOR = new Delegator();

	private static int scriptCounter = 0;

	private ScriptBindings bindings;

	public GroovyScriptHelper(ScriptBindings bindings) {
		this.bindings = bindings;
	}

	public GroovyScriptHelper(Context context) {
		this(new ScriptBindings(context));
	}

	@Override
	public ScriptBindings getBindings() {
		return bindings;
	}

	@Override
	public void setBindings(ScriptBindings bindings) {
		this.bindings = bindings;
	}

	private Object enhancer(Class<?> forClass, String expr) {

		final String key = forClass.getName() + expr;
		Object enhanced = CACHE.getIfPresent(key);

		if (enhanced != null) {
			return enhanced;
		}

		final Enhancer enhancer = new Enhancer();

		final String name = "ScriptClass" + scriptCounter++;
		final String code = generate(forClass, expr, name);

		final Class<?> script = SHELL.getClassLoader().parseClass(code, name + ".groovy");

		// Ask shell class loader to clear internal cache to ensure GC
		// can claim them. Fixes PermGen error.
		SHELL.getClassLoader().clearCache();

		enhancer.setSuperclass(script);

		Callback[] callbacks = {
			DELEGATOR, NoOp.INSTANCE
		};

		enhancer.setCallbacks(callbacks);
		enhancer.setCallbackFilter(DELEGATOR);

		enhanced = enhancer.create();
		CACHE.put(key, enhanced);

		return enhanced;
	}

	@SuppressWarnings("all")
	public Object evalStatic(String expr) {

		Object orig = bindings.get("__this__");
		if (!(orig instanceof Model)) {
			return evalDynamic(expr);
		}

		Class<?> type = orig.getClass();
		Scriptable<Model> script;

		try {
			script = (Scriptable<Model>) enhancer(type, expr);
		} catch (Exception e) {
			return evalDynamic(expr);
		}

		Model __this__ = expr.contains("__this__") ? (Model) bindings.get("__this__") : null;
		Model __self__ = expr.contains("__self__") ? (Model) bindings.get("__self__") : null;
		User __user__ = expr.contains("__user__") ? (User) bindings.get("__user__") : null;

		Context __parent__ = expr.contains("__parent__") ? (Context) bindings.get("__parent__") : null;
		Model __ref__ = expr.contains("__ref__") ? (Model) bindings.get("__ref__") : null;

		LocalDate __date__ = expr.contains("__date__") ? (LocalDate) bindings.get("__date__") : null;
		LocalDateTime __time__ = expr.contains("__time__") ? (LocalDateTime) bindings.get("__time__") : null;
		DateTime __datetime__ = expr.contains("__datetime__") ? (DateTime) bindings.get("__datetime__") : null;

		DELEGATOR.set(orig);
		try {
			return script.__eval(__this__, __self__, __user__, __parent__, __ref__, __date__, __time__, __datetime__);
		} finally {
			DELEGATOR.clear();
		}
	}

	public Object evalDynamic(String expr) {
		String key = "dynamic:" + expr;
		Class<?> klass = (Class<?>) CACHE.getIfPresent(key);
		try {
			if (klass == null) {
				klass = SHELL.getClassLoader().parseClass(expr);
				CACHE.put(key, klass);
			}
			Script script = (Script) klass.newInstance();
			script.setBinding(new Binding(bindings) {

				@Override
				public Object getVariable(String name) {
					try {
						return super.getVariable(name);
					} catch (MissingPropertyException e) {
					}
					return null;
				}
			});
			return script.run();
		} catch (Exception e) {
		}
		return null;
	}

	@Override
	public Object eval(String expr) {
		return evalStatic(expr);
	}

	@Override
	public final boolean test(String expr) {
		if (Strings.isNullOrEmpty(expr))
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
	public Object call(Object obj, String method, Object... args) {
		Preconditions.checkNotNull(obj);
		Preconditions.checkNotNull(method);
		return InvokerHelper.invokeMethod(obj, method, args);
	}

	@Override
	public Object call(Object obj, String methodCall) {
		Preconditions.checkNotNull(obj);
		Preconditions.checkNotNull(methodCall);

		Pattern p = Pattern.compile("(\\w+)\\((.*?)\\)");
		Matcher m = p.matcher(methodCall);

		if (!m.matches()) return null;

		String method = m.group(1);
		String params = "[" + m.group(2) + "] as Object[]";
		Object[] arguments = (Object[]) eval(params);

		return call(obj, method, arguments);
	}

	private String generate(Class<?> forClass, String expr, String scriptName) {
		String scriptPackage = forClass.getPackage().getName();
		String superClass = forClass.getName();
		return String.format(TEMPLATE, scriptPackage, scriptName, superClass,
				superClass, superClass, superClass, expr);
	}

	private static final String TEMPLATE  = ""
			+ "package %s;\n"
			+ "\n"
			+ "@groovy.transform.CompileStatic\n"
			+ "public class %s extends %s implements com.axelor.script.Scriptable<%s> {\n"
			+ "\n"
			+ "\tpublic final Object __eval("
			+ "%s __this__, "
			+ "%s __self__, "
			+ "com.axelor.auth.db.User __user__, "
			+ "com.axelor.rpc.Context __parent__, "
			+ "com.axelor.db.Model __ref__, "
			+ "org.joda.time.LocalDate __date__, "
			+ "org.joda.time.LocalDateTime __time__, "
			+ "org.joda.time.DateTime __datetime__) {\n"
			+ "\t\treturn %s;\n"
			+ "\t}\n"
			+ "}";

	static class Delegator implements MethodInterceptor, CallbackFilter {

		private static final Set<String> IGNORE = ImmutableSet.of("__eval", "invokeMethod", "getMetaClass", "getClass");

		private static final ThreadLocal<Object> DELEGATE = new ThreadLocal<Object>();

		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			if (IGNORE.contains(method.getName())) {
				return proxy.invokeSuper(obj, args);
			}
			Object delegate = DELEGATE.get();
			if (delegate == null) {
				throw new IllegalAccessError("no delegate object");
			}
			return method.invoke(delegate, args);
		}

		@Override
		public int accept(Method method) {
			if (IGNORE.contains(method.getName()) ||
				!Modifier.isPublic(method.getModifiers()) ||
				method.getDeclaringClass().getName().startsWith("ScriptClass")) {
				return 1;
			}
			if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
				return 0;
			}
			return 1;
		}

		public void set(Object delegate) {
			DELEGATE.set(delegate);
		}

		public void clear() {
			DELEGATE.remove();
			DELEGATE.set(null);
		}
	}
}
