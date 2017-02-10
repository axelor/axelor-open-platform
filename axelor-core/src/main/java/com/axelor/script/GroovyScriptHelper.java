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

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;

import java.util.concurrent.TimeUnit;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.db.JpaScanner;
import com.axelor.rpc.Context;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class GroovyScriptHelper extends AbstractScriptHelper {

	private static final CompilerConfiguration config = new CompilerConfiguration();

	private static final int DEFAULT_CACHE_SIZE = 500;
	private static final int DEFAULT_CACHE_EXPIRE_TIME = 60;

	private static int cacheSize;
	private static int cacheExpireTime;

	private static final GroovyClassLoader GCL;
	private static final Cache<String, Class<?>> SCRIPT_CACHE;

	private static Logger log = LoggerFactory.getLogger(GroovyScriptHelper.class);

	static {
		config.getOptimizationOptions().put("indy", Boolean.TRUE);
		config.getOptimizationOptions().put("int", Boolean.FALSE);

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

		SCRIPT_CACHE = CacheBuilder.newBuilder()
				.maximumSize(cacheSize)
				.expireAfterAccess(cacheExpireTime, TimeUnit.MINUTES)
				.build();

		GCL = new GroovyClassLoader(JpaScanner.getClassLoader(), config);
	}

	public GroovyScriptHelper(ScriptBindings bindings) {
		this.setBindings(bindings);
	}

	public GroovyScriptHelper(Context context) {
		this(new ScriptBindings(context));
	}

	private Class<?> parseClass(String code) {

		Class<?> klass = SCRIPT_CACHE.getIfPresent(code);
		if (klass != null) {
			return klass;
		}

		try {
			klass = GCL.parseClass(code);
		} finally {
			GCL.clearCache();
		}

		SCRIPT_CACHE.put(code, klass);

		return klass;
	}

	@Override
	public Object eval(String expr) {
		try {
			Class<?> klass = parseClass(expr);
			Script script = (Script) klass.newInstance();
			script.setBinding(new Binding(getBindings()) {

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
			log.error("Script error: {}", expr, e);
		}
		return null;
	}

	@Override
	protected Object doCall(Object obj, String methodCall) {
		ScriptBindings bindings = new ScriptBindings(getBindings());
		GroovyScriptHelper sh = new GroovyScriptHelper(bindings);
		bindings.put("__obj__", obj);
		return sh.eval("__obj__." + methodCall);
	}
}
