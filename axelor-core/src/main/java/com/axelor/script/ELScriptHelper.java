/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import com.axelor.db.JpaRepository;
import com.axelor.db.JpaScanner;
import com.axelor.db.Model;
import com.axelor.db.ValueEnum;
import com.axelor.inject.Beans;
import com.axelor.rpc.Context;
import com.axelor.rpc.ContextEntity;
import com.google.common.primitives.Ints;
import jakarta.el.BeanELResolver;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELProcessor;
import jakarta.el.ImportHandler;
import jakarta.el.MapELResolver;
import jakarta.el.MethodNotFoundException;
import java.lang.reflect.Method;
import java.util.Map;
import javax.script.Bindings;

public class ELScriptHelper extends AbstractScriptHelper {

  private ELProcessor processor;

  private static final ScriptPolicy SCRIPT_POLICY = ScriptPolicy.getInstance();

  class ClassResolver extends MapELResolver {

    private static final String FIELD_CLASS = "class";

    public ClassResolver() {
      super(true);
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) {

      if (base instanceof ELClass elClass && FIELD_CLASS.equals(property)) {
        Class<?> cls = elClass.getKlass();
        SCRIPT_POLICY.check(cls);
        context.setPropertyResolved(true);
        return cls;
      }

      if (base != null) {
        return null;
      }

      // try resolving model/repository classes
      Class<?> cls = JpaScanner.findModel(property.toString());
      if (cls == null) {
        cls = JpaScanner.findRepository(property.toString());
      }
      if (cls == null) {
        cls = JpaScanner.findEnum(property.toString());
      }
      if (cls == null) {
        return null;
      }

      SCRIPT_POLICY.check(cls);
      context.setPropertyResolved(true);

      return cls;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {}
  }

  class ContextResolver extends MapELResolver {

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
      final Bindings bindings = getBindings();
      if (bindings == null || base != null) {
        return null;
      }

      final String name = (String) property;
      if (bindings.containsKey(name)) {
        context.setPropertyResolved(true);
        return bindings.get(name);
      }

      final ImportHandler handler = context.getImportHandler();
      Object value = handler.resolveClass(name);

      if (value instanceof Class) SCRIPT_POLICY.check((Class<?>) value);
      if (value == null) {
        value = handler.resolveStatic(name);
      }
      context.setPropertyResolved(true);
      return value;
    }
  }

  class BeanResolver extends BeanELResolver {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Object getValue(ELContext context, Object base, Object property) {
      if (base instanceof Map<?, ?> map
          && !(base instanceof ContextEntity)
          && map.containsKey(property)) {
        context.setPropertyResolved(true);
        return map.get(property);
      }

      if (base instanceof Class<?> cls) {
        // is value enum?
        if (ValueEnum.class.isAssignableFrom(cls)) {
          context.setPropertyResolved(true);
          return ValueEnum.of((Class) base, property);
        }

        // access static member
        if (property instanceof String propName) {
          try {
            Object value = SCRIPT_POLICY.check(cls).getField(propName).get(cls);
            context.setPropertyResolved(true);
            return value;
          } catch (Exception e) {
            return null;
          }
        }
      }

      // access instance member
      if (base != null && property instanceof String propName) {
        try {
          Object value = SCRIPT_POLICY.check(base.getClass()).getField(propName).get(base);
          context.setPropertyResolved(true);
          return value;
        } catch (Exception e) {
          return null;
        }
      }

      return null;
    }

    @Override
    public Object invoke(
        ELContext context,
        final Object base,
        Object method,
        Class<?>[] paramTypes,
        Object[] params) {
      if (base instanceof Class<?> clazz) {
        final Class<?> klass = SCRIPT_POLICY.check(clazz);
        try {
          final Method staticMethod = klass.getMethod(method.toString(), paramTypes);
          context.setPropertyResolved(true);
          return staticMethod.invoke(klass, params);
        } catch (NoSuchMethodException | SecurityException e) {
          throw new MethodNotFoundException(klass.getName() + "." + method.toString());
        } catch (IllegalArgumentException e) {
          throw e;
        } catch (Exception e) {
          throw new ELException(e);
        }
      }
      return null;
    }
  }

  public static final class Helpers {

    private static Class<?> typeClass1(Object type) {
      if (type instanceof Class<?> klass) return klass;
      if (type instanceof String)
        try {
          return Class.forName(type.toString());
        } catch (ClassNotFoundException e) {
          throw new IllegalArgumentException(e);
        }
      if (type instanceof ELClass elClass) return elClass.getKlass();
      throw new IllegalArgumentException("Invalid type: " + type);
    }

    private static Class<?> typeClass(Object type) {
      return SCRIPT_POLICY.check(typeClass1(type));
    }

    public static Object as(Object base, Object type) {
      final Class<?> klass = typeClass(type);
      if (base instanceof Context context) {
        return context.asType(klass);
      }
      return klass.cast(base);
    }

    public static Object is(Object base, Object type) {
      final Class<?> klass = typeClass(type);
      if (base instanceof Context context) {
        return klass.isAssignableFrom(context.getContextClass());
      }
      return klass.isInstance(base);
    }

    public static Class<?> importClass(String name) {
      try {
        return SCRIPT_POLICY.check(Class.forName(name));
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(e);
      }
    }

    public static <T extends Model> JpaRepository<T> repo(Class<T> model) {
      return JpaRepository.of(SCRIPT_POLICY.check(model));
    }

    public static <T> T bean(Class<T> type) {
      return Beans.get(SCRIPT_POLICY.check(type));
    }

    public static Integer toInt(Object value) {
      if (value == null) {
        return null;
      }
      if (value instanceof Integer integer) {
        return integer;
      }
      if (value instanceof Long longValue) {
        return Ints.checkedCast(longValue);
      }
      return Integer.valueOf(value.toString());
    }

    public static String text(Object value) {
      if (value == null) return "";
      return value.toString();
    }

    public static String formatText(String format, Object... args) {
      if (args == null) {
        return format.formatted(args);
      }
      return format.formatted(args);
    }
  }

  public ELScriptHelper(Bindings bindings) {

    this.processor = new ELProcessor();
    this.processor.getELManager().addELResolver(new ClassResolver());
    this.processor.getELManager().addELResolver(new ContextResolver());
    this.processor.getELManager().addELResolver(new BeanResolver());

    final String className = Helpers.class.getName();

    try {
      this.processor.defineFunction("", "as", className, "as");
      this.processor.defineFunction("", "is", className, "is");
      this.processor.defineFunction("", "int", className, "toInt");
      this.processor.defineFunction("", "str", className, "text");
      this.processor.defineFunction("", "imp", className, "importClass");
      this.processor.defineFunction("", "T", className, "importClass");
      this.processor.defineFunction("", "__repo__", className, "repo");
      this.processor.defineFunction("", "__bean__", className, "bean");
      this.processor.defineFunction("fmt", "text", className, "formatText");
    } catch (Exception e) {
    }

    final String[] packages = {
      "java.util", "java.time",
    };

    for (String pkg : packages) {
      try {
        this.processor.getELManager().importPackage(pkg);
      } catch (Exception e) {
      }
    }

    this.processor.getELManager().importClass("com.axelor.db.Model");
    this.processor.getELManager().importClass("com.axelor.db.Query");
    this.processor.getELManager().importClass("com.axelor.db.Repository");

    this.setBindings(bindings);
  }

  public ELScriptHelper(Context context) {
    this(new ScriptBindings(context));
  }

  @Override
  public Object eval(String expr, Bindings bindings) {
    final Bindings current = getBindings();
    try {
      setBindings(bindings);
      return processor.eval(expr);
    } finally {
      setBindings(current);
    }
  }
}
