/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.data;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * This {@link ScriptHelper} class can be used to effectively use the
 * {@link GroovyShell} to evaluate dymanic groovy expressions. <br>
 * <br>
 * The {@link ScriptHelper} maintains an internal LRU cache to reuse the parsed
 * script. If the specified expiry time is elapsed without any access to the
 * cached script, the cache is evited to regain the memory.
 * 
 */
public final class ScriptHelper {

	private static final int DEFAULT_CACHE_SIZE = 100;

	private static final int DEFAULT_EXPIRE_TIME = 10;

	private static final Logger log = LoggerFactory.getLogger(ScriptHelper.class);
	
	final static CompilerConfiguration config = new CompilerConfiguration();

	static {
		config.getOptimizationOptions().put("indy", true);
		config.getOptimizationOptions().put("int", false);
	}
	
	private boolean indy = true;
	
	private int cacheSize = DEFAULT_CACHE_SIZE;
	
	private int expireTime = DEFAULT_EXPIRE_TIME;

	private LoadingCache<String, Script> cache = CacheBuilder
			.newBuilder()
			.maximumSize(cacheSize)
			.expireAfterAccess(expireTime, TimeUnit.MINUTES)
			.build(new CacheLoader<String, Script>() {

				@Override
				public Script load(String expr) throws Exception {
					if (indy) {
						return new GroovyShell(config).parse(expr);
					}
					return new GroovyShell().parse(expr);
				}
			});

	/**
	 * Create a new {@link ScriptHelper} with the given {@link #cacheSize} and
	 * {@link #expireTime} in minutes.
	 * 
	 * @param cacheSize
	 *            Size of the cache.
	 * @param expireTime
	 *            The expire time in minutes after which the script is evicted
	 *            from the cache if not accessed.
	 * @param indy
	 *            Whether to use the invoke dynamic feature.
	 */
	public ScriptHelper(int cacheSize, int expireTime, boolean indy) {
		this.cacheSize = cacheSize;
		this.expireTime = expireTime;
		this.indy = indy;
	}

	/**
	 * Evaluate the given expression.<br>
	 * <br>
	 * An instance of {@link Binding} is created to deal with missing variables.
	 * In that case, <code>null</code> is returned and avoids
	 * {@link MissingPropertyException}.
	 * 
	 * @param expression
	 *            the groovy expression to evaluate
	 * @param variables
	 *            the binding variables
	 * @return the result of the expression
	 */
	public Object eval(String expression, Map<String, Object> variables) {
		
		return eval(expression, new Binding(variables) {
			
			@Override
			public Object getVariable(String name) {
				try {
					return super.getVariable(name);
				} catch (MissingPropertyException e){
					return null;
				}
			}
		});
	}

	/**
	 * Evaluate the given expression.
	 * 
	 * @param expression
	 *            the groovy expression to evaluate
	 * @param binding
	 *            the binding variables
	 * @return the result of the expression
	 */
	public Object eval(String expression, Binding binding) {
		Script script;
		try {
			script = cache.get(expression);
		} catch (ExecutionException e) {
			log.warn("Invalid script: {}", expression);
			return null;
		}
		try {
			script.setBinding(binding);
			return script.run();
		} finally {
			script.setBinding(null);
		}
	}
}
