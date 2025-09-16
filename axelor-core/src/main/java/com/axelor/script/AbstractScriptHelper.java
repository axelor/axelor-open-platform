/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.Bindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractScriptHelper implements ScriptHelper {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  private Bindings bindings;

  @Override
  public Bindings getBindings() {
    return bindings;
  }

  @Override
  public void setBindings(Bindings bindings) {
    this.bindings = bindings;
  }

  @Override
  public final boolean test(String expr) {
    if (StringUtils.isBlank(expr)) return true;

    if ("true".equals(expr)) return true;
    if ("false".equals(expr)) return false;

    Object result = eval(expr);
    if (result == null) return false;
    if (result instanceof Boolean booleanResult) return booleanResult;
    if (result instanceof Number numberResult)
      return Double.compare(numberResult.doubleValue(), 0) != 0;

    return ObjectUtils.notEmpty(result);
  }

  @Override
  public Object call(Object obj, String methodCall) {
    Objects.requireNonNull(obj);
    Objects.requireNonNull(methodCall);

    Pattern p = Pattern.compile("(\\w+)\\((.*?)\\)");
    Matcher m = p.matcher(methodCall);

    if (!m.matches()) {
      return null;
    }

    return doCall(obj, methodCall);
  }

  protected Object doCall(Object obj, String methodCall) {
    final String key = "__obj__" + Math.abs(UUID.randomUUID().getMostSignificantBits());
    final Bindings bindings = getBindings();
    try {
      bindings.put(key, obj);
      return eval(key + "." + methodCall);
    } finally {
      bindings.remove(key);
    }
  }

  @Override
  public Object eval(String expr) {
    try {
      return eval(expr, getBindings());
    } catch (NoSuchFieldException e) {
      log.warn("No such field in: {} -- ({})", expr, e.getMessage());
      return null;
    } catch (Exception e) {
      if (e.getCause() instanceof NoSuchFieldException) {
        log.warn("No such field in: {} -- ({})", expr, e.getMessage());
        return null;
      }
      log.error("Script error: {}", expr, e);
      throw new IllegalArgumentException(e);
    }
  }
}
