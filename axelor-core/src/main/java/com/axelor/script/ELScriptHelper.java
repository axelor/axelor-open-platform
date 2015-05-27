/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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

import com.axelor.common.ClassUtils;
import com.axelor.db.JpaRepository;
import com.axelor.db.JpaScanner;
import com.axelor.db.Model;
import com.axelor.internal.javax.el.ELClass;
import com.axelor.internal.javax.el.ELContext;
import com.axelor.internal.javax.el.ELProcessor;
import com.axelor.internal.javax.el.ImportHandler;
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

			final String name = (String) property;
			if (bindings.containsKey(name))  {
				context.setPropertyResolved(true);
				return bindings.get(name);
			}

			final ImportHandler handler = context.getImportHandler();
			Object value = handler.resolveClass(name);
			if (value == null) {
				value = handler.resolveStatic(name);
			}
			context.setPropertyResolved(true);
			return value;
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

		public static Class<?> importClass(String name) {
			return ClassUtils.findClass(name);
		}

		public static <T extends Model> JpaRepository<T> repo(Class<T> model) {
			return JpaRepository.of(model);
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

		final String className = Helpers.class.getName();

		try {
			this.processor.defineFunction("", "as", className, "as");
			this.processor.defineFunction("", "is", className, "is");
			this.processor.defineFunction("", "int", className, "toInt");
			this.processor.defineFunction("", "str", className, "text");
			this.processor.defineFunction("", "imp", className, "importClass");
			this.processor.defineFunction("", "__repo__", className, "repo");
			this.processor.defineFunction("fmt", "text", className, "formatText");
		} catch (Exception e) {
		}

		final String[] packages = {
			"java.util",
			"org.joda.time",
			"com.axelor.common",
			"com.axelor.script.util",
			"com.axelor.apps.tool"
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
