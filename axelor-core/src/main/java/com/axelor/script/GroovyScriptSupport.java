/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import com.axelor.app.AvailableAppSettings;
import com.axelor.db.EntityHelper;
import com.axelor.db.JpaRepository;
import com.axelor.db.JpaScanner;
import com.axelor.db.Model;
import com.axelor.inject.Beans;
import com.axelor.text.AxelorGStringTemplateEngine;
import com.axelor.text.AxelorStreamingTemplateEngine;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import groovy.text.GStringTemplateEngine;
import groovy.text.StreamingTemplateEngine;
import java.lang.reflect.Executable;
import java.util.concurrent.TimeUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

public class GroovyScriptSupport {

  private static final CompilerConfiguration config = new CompilerConfiguration();

  private static final int DEFAULT_CACHE_SIZE = 500;
  private static final int DEFAULT_CACHE_EXPIRE_TIME = 60;

  private static int cacheSize;
  private static int cacheExpireTime;

  private static final GroovyClassLoader GCL;
  private static final LoadingCache<String, Class<?>> SCRIPT_CACHE;

  private static final ScriptPolicy SCRIPT_POLICY;

  public static class Helpers {

    public static JpaRepository<? extends Model> getRepo(Class<?> klass) {
      Class<?> k =
          Model.class.isAssignableFrom(klass) ? klass : JpaScanner.findModel(klass.getSimpleName());
      return JpaRepository.of(SCRIPT_POLICY.check(k).asSubclass(Model.class));
    }

    public static <T> T getBean(Class<T> type) {
      T obj = Beans.get(type);
      Class<T> klass = EntityHelper.getEntityClass(obj);
      SCRIPT_POLICY.check(klass);
      return obj;
    }
  }

  public static class PolicyChecker {

    public static final String NAME = "__$$policy";

    public static final String CALL_CHECK = "check";
    public static final String CALL_TIMEOUT = "timeout";

    private long start;

    private long timeout;

    public PolicyChecker() {
      this(SCRIPT_POLICY.getTimeout());
    }

    public PolicyChecker(long timeout) {
      this.timeout = timeout;
      this.start = System.currentTimeMillis();
    }

    public void timeout() {
      if (System.currentTimeMillis() - start > timeout) {
        throw new ScriptTimeoutException();
      }
    }

    public <T> T check(T obj) {
      if (obj == null) return null;

      Class<?> klass = obj.getClass();

      if (obj instanceof Class<?>) klass = (Class<?>) obj;
      if (obj instanceof Executable) klass = ((Executable) obj).getDeclaringClass();

      if (obj instanceof Script) {
        return obj;
      }

      SCRIPT_POLICY.check(klass);

      return obj;
    }
  }

  static {
    config.getOptimizationOptions().put("indy", Boolean.TRUE);
    config.getOptimizationOptions().put("int", Boolean.FALSE);

    final ImportCustomizer importCustomizer = new ImportCustomizer();

    importCustomizer.addStaticImport("__repo__", Helpers.class.getName(), "getRepo");
    importCustomizer.addStaticImport("__bean__", Helpers.class.getName(), "getBean");

    importCustomizer.addImports("java.time.ZonedDateTime");
    importCustomizer.addImports("java.time.LocalDateTime");
    importCustomizer.addImports("java.time.LocalDate");
    importCustomizer.addImports("java.time.LocalTime");

    final ASTTransformationCustomizer astCustomizer =
        new ASTTransformationCustomizer(GroovyCheck.class);
    final ASTTransformationCustomizer astPropCustomizer =
        new ASTTransformationCustomizer(GroovyCheckProp.class);

    config.addCompilationCustomizers(importCustomizer);
    config.addCompilationCustomizers(astCustomizer);
    config.addCompilationCustomizers(astPropCustomizer);

    try {
      cacheSize =
          Integer.parseInt(System.getProperty(AvailableAppSettings.APPLICATION_SCRIPT_CACHE_SIZE));
    } catch (Exception e) {
    }
    try {
      cacheExpireTime =
          Integer.parseInt(
              System.getProperty(AvailableAppSettings.APPLICATION_SCRIPT_CACHE_EXPIRE_TIME));
    } catch (Exception e) {
    }

    if (cacheSize <= 0) {
      cacheSize = DEFAULT_CACHE_SIZE;
    }
    if (cacheExpireTime <= 0) {
      cacheExpireTime = DEFAULT_CACHE_EXPIRE_TIME;
    }

    GCL = new GroovyClassLoader(JpaScanner.getClassLoader(), config);

    SCRIPT_POLICY = ScriptPolicy.getInstance();

    SCRIPT_CACHE =
        Caffeine.newBuilder()
            .maximumSize(cacheSize)
            .expireAfterAccess(cacheExpireTime, TimeUnit.MINUTES)
            .build(
                code -> {
                  try {
                    return GCL.parseClass(code);
                  } finally {
                    GCL.clearCache();
                  }
                });
  }

  public static Script createScript(String script) {
    try {
      Class<?> klass = SCRIPT_CACHE.get(script);
      return (Script) klass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static GStringTemplateEngine createStringTemplateEngine() {
    return new AxelorGStringTemplateEngine(JpaScanner.getClassLoader(), config);
  }

  public static StreamingTemplateEngine createStreamingTemplateEngine() {
    return new AxelorStreamingTemplateEngine(JpaScanner.getClassLoader(), config);
  }
}
