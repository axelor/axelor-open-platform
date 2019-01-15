/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.data;

import com.axelor.db.JpaRepository;
import com.axelor.db.JpaScanner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link DataScriptHelper} class can be used to effectively use the {@link GroovyShell} to
 * evaluate dynamic groovy expressions. <br>
 * <br>
 * The {@link DataScriptHelper} maintains an internal LRU cache to reuse the parsed script. If the
 * specified expiry time is elapsed without any access to the cached script, the cache is evicted to
 * regain the memory.
 */
public final class DataScriptHelper {

  private static final int DEFAULT_CACHE_SIZE = 100;

  private static final int DEFAULT_EXPIRE_TIME = 10;

  private static final Logger log = LoggerFactory.getLogger(DataScriptHelper.class);

  private static final CompilerConfiguration config = new CompilerConfiguration();
  private static final CompilerConfiguration configIndy = new CompilerConfiguration();

  static {
    final ImportCustomizer importCustomizer = new ImportCustomizer();
    importCustomizer.addStaticImport("__repo__", JpaRepository.class.getName(), "of");

    configIndy.getOptimizationOptions().put("indy", true);
    configIndy.getOptimizationOptions().put("int", false);
    configIndy.addCompilationCustomizers(importCustomizer);
    config.addCompilationCustomizers(importCustomizer);
  }

  private boolean indy = true;

  private int cacheSize = DEFAULT_CACHE_SIZE;

  private int expireTime = DEFAULT_EXPIRE_TIME;

  private LoadingCache<String, Script> cache =
      CacheBuilder.newBuilder()
          .maximumSize(cacheSize)
          .expireAfterAccess(expireTime, TimeUnit.MINUTES)
          .build(
              new CacheLoader<String, Script>() {

                @Override
                public Script load(String expr) throws Exception {
                  final CompilerConfiguration cfg = indy ? configIndy : config;
                  return new GroovyShell(JpaScanner.getClassLoader(), new Binding(), cfg)
                      .parse(expr);
                }
              });

  /**
   * Create a new {@link DataScriptHelper} with the given {@link #cacheSize} and {@link #expireTime}
   * in minutes.
   *
   * @param cacheSize Size of the cache.
   * @param expireTime The expire time in minutes after which the script is evicted from the cache
   *     if not accessed.
   * @param indy Whether to use the invoke dynamic feature.
   */
  public DataScriptHelper(int cacheSize, int expireTime, boolean indy) {
    this.cacheSize = cacheSize;
    this.expireTime = expireTime;
    this.indy = indy;
  }

  /**
   * Evaluate the given expression.<br>
   * <br>
   * An instance of {@link Binding} is created to deal with missing variables. In that case, <code>
   * null</code> is returned and avoids {@link MissingPropertyException}.
   *
   * @param expression the groovy expression to evaluate
   * @param variables the binding variables
   * @return the result of the expression
   */
  public synchronized Object eval(String expression, Map<String, Object> variables) {

    return eval(
        expression,
        new Binding(variables) {

          @Override
          public Object getVariable(String name) {
            try {
              return super.getVariable(name);
            } catch (MissingPropertyException e) {
              return null;
            }
          }
        });
  }

  /**
   * Evaluate the given expression.
   *
   * @param expression the groovy expression to evaluate
   * @param binding the binding variables
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
