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
import java.util.concurrent.TimeUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.messages.ExceptionMessage;

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

    // Scripts and templates extend SandboxScript, which denies the Script methods escaping the
    // sandbox (evaluate...) and marks the code as sandboxed (see SandboxGroovyMethods).
    config.setScriptBaseClass(SandboxScript.class.getName());

    // Closures extend SandboxClosure, which denies delegates and owners not allowed by the policy.
    config.setBytecodePostprocessor(new SandboxClosureProcessor());

    config.addCompilationCustomizers(importCustomizer);
    config.addCompilationCustomizers(astCustomizer);
    config.addCompilationCustomizers(astPropCustomizer);
    config.addCompilationCustomizers(new GroovyImportCheck());

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
                  } catch (MultipleCompilationErrorsException e) {
                    throw policyError(e);
                  } finally {
                    GCL.clearCache();
                  }
                });
  }

  /**
   * Unwraps a policy violation detected at compile time (see {@link GroovyImportCheck}), so it is
   * reported as a {@link ScriptPolicyException} rather than a generic compilation error.
   */
  private static RuntimeException policyError(MultipleCompilationErrorsException e) {
    for (Object error : e.getErrorCollector().getErrors()) {
      if (error instanceof ExceptionMessage message
          && message.getCause() instanceof ScriptPolicyException policyException) {
        return policyException;
      }
    }
    return e;
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
