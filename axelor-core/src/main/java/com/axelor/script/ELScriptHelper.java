package com.axelor.script;

import com.axelor.common.ClassUtils;
import com.axelor.db.JpaScanner;
import com.axelor.internal.javax.el.ELClass;
import com.axelor.internal.javax.el.ELContext;
import com.axelor.internal.javax.el.ELProcessor;
import com.axelor.internal.javax.el.MapELResolver;
import com.axelor.rpc.Context;
import com.google.common.primitives.Ints;

public class ELScriptHelper extends AbstractScriptHelper {

	private ELProcessor processor;

	class ClassResolver extends MapELResolver {

		private static final String FIELD_CLASS = "class";

		public ClassResolver() {
			super(true);
		}

		@Override
		public Object getValue(ELContext context, Object base, Object property) {

			if (base instanceof ELClass && FIELD_CLASS.equals(property)) {
				context.setPropertyResolved(true);
				return ((ELClass) base).getKlass();
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
				return null;
			}

			context.setPropertyResolved(true);
			return cls;
		}
	}

	class ContextResolver extends MapELResolver {

		@Override
		public Object getValue(ELContext context, Object base, Object property) {
			final ScriptBindings bindings = getBindings();
			if (bindings == null || base != null) {
				return null;
			}
			context.setPropertyResolved(true);
			return bindings.get(property);
		}
	}

	public static final class Helpers {

		private static Class<?> typeClass(Object type) {
			if (type instanceof Class<?>) return (Class<?>) type;
			if (type instanceof String) return ClassUtils.findClass(type.toString());
			if (type instanceof ELClass) return ((ELClass) type).getKlass();
			throw new IllegalArgumentException("Invalid type: " + type);
		}

		public static Object as(Object base, Object type) {
			final Class<?> klass = typeClass(type);
			if (base instanceof Context) {
				return ((Context) base).asType(klass);
			}
			return klass.cast(base);
		}

		public static Object is(Object base, Object type) {
			final Class<?> klass = typeClass(type);
			if (base instanceof Context) {
				return klass.isAssignableFrom(((Context) base).getContextClass());
			}
			return klass.isInstance(base);
		}

		public static Integer toInt(Object value) {
			if (value == null) {
				return null;
			}
			if (value instanceof Integer) {
				return (Integer) value;
			}
			if (value instanceof Long) {
				return Ints.checkedCast((Long) value);
			}
			return Integer.valueOf(value.toString());
		}

		public static String text(Object value) {
			if (value == null) return "";
			return value.toString();
		}

		public static String formatText(String format, Object... args) {
			if (args == null) {
				return String.format(format, args);
			}
			return String.format(format, args);
		}
	}

	public ELScriptHelper(ScriptBindings bindings) {

		this.processor = new ELProcessor();
		this.processor.getELManager().addELResolver(new ClassResolver());
		this.processor.getELManager().addELResolver(new ContextResolver());

		try {
			this.processor.defineFunction("", "as", Helpers.class.getName(), "as");
			this.processor.defineFunction("", "is", Helpers.class.getName(), "is");
			this.processor.defineFunction("", "int", Helpers.class.getName(), "toInt");
			this.processor.defineFunction("", "str", Helpers.class.getName(), "text");
			this.processor.defineFunction("fmt", "text", Helpers.class.getName(), "formatText");
		} catch (Exception e) {
		}

		this.processor.getELManager().importPackage("java.util");
		this.processor.getELManager().importPackage("org.joda.time");

		this.setBindings(bindings);
	}

	public ELScriptHelper(Context context) {
		this(new ScriptBindings(context));
	}

	@Override
	public Object eval(String expr) {
		return processor.eval(expr);
	}

	@Override
	protected Object doCall(Object obj, String methodCall) {
		ScriptBindings bindings = new ScriptBindings(getBindings());
		ELScriptHelper sh = new ELScriptHelper(bindings);
		bindings.put("__obj__", obj);
		return sh.eval("__obj__." + methodCall);
	}
}
