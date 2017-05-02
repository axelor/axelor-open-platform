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
package com.axelor.rpc;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.axelor.common.ResourceUtils;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.internal.asm.ClassReader;
import com.axelor.internal.asm.ClassVisitor;
import com.axelor.internal.asm.MethodVisitor;
import com.axelor.internal.asm.Opcodes;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * This {@link ContextProxy} provides seamless way to access context values
 * using proxy.
 * 
 * <p>
 * The proxy and it's fields are initialized lazily from the context value map
 * when context variable is access. Any missing value of the bean is accessed
 * from the managed instance.
 * 
 * <p>
 * For internal use only.
 * 
 * @see Context
 */
public class ContextProxy<T> {

	private static final String FIELD_ID = "id";
	private static final String COMPUTE_PREFIX = "compute";

	private Map<String, Property> fieldsByMethod;
	private Map<String, Set<String>> computeDependencies;

	private final Map<String, Object> values;
	private final Set<String> validated;

	private final Class<T> beanClass;

	private Model managed;
	private Model unmanaged;

	private boolean searched;

	private ContextProxy(Map<String, Object> values, Class<T> beanClass) {
		this.values = Objects.requireNonNull(values);
		this.beanClass = Objects.requireNonNull(beanClass);
		this.validated = new HashSet<>();
	}
	
	private Property findProperty(String methodName) {
		if (fieldsByMethod == null) {
			fieldsByMethod = new HashMap<>();
			final Mapper mapper = Mapper.of(beanClass);
			for (Property property : mapper.getProperties()) {
				final String name = property.getName();
				final Method getter = mapper.getGetter(name);
				final Method setter = mapper.getSetter(name);
				if (getter != null) fieldsByMethod.put(getter.getName(), property);
				if (setter != null) fieldsByMethod.put(setter.getName(), property);
			}
		}
		return fieldsByMethod.get(methodName);
	}

	private Long findId() {
		try {
			return Long.parseLong(values.get(FIELD_ID).toString());
		} catch (Exception e) {
			return null;
		}
	}

	private Model managed() {
		if (searched) {
			return managed;
		}
		final Long id = findId();
		if (id != null) {
			managed = (Model) JPA.em().find(beanClass, id);
		}
		searched = true;
		return managed;
	}

	private Model unmanaged() {
		if (unmanaged == null) {
			unmanaged = (Model) Mapper.toBean(beanClass, null);
		}
		return unmanaged;
	}

	@SuppressWarnings("unchecked")
	private Object createOrFind(Property property, Object item) {
		if (item == null || item instanceof Model) {
			return item;
		}
		if (item instanceof Map) {
			return create((Map<String, Object>) item, property.getTarget());
		}
		if (item instanceof Number) {
			return JPA.em().find(property.getTarget(), item);
		}
		throw new IllegalArgumentException("Invalid collection item for field: " + property.getName());
	}

	private void validate(Property property) {
		Object value = values.get(property.getName());
		if (value == null || validated.contains(property.getName())) {
			return;
		}
		if (property.isCollection() && value instanceof Collection) {
			value = ((Collection<?>) value).stream()
					.map(item -> createOrFind(property, item))
					.collect(Collectors.toList());
		} else if (property.isReference()) {
			value = createOrFind(property, value);
		}

		final Mapper mapper = Mapper.of(beanClass);
		final Object bean = unmanaged();
		if (property.getTarget() != null) {
			// prevent automatic association handling
			// causing detached entity exception
			mapper.set(bean, property.getName(), value);
		} else {
			property.set(bean, value);
		}

		validated.add(property.getName());
	}
	
	private String getComputedField(String methodName) {
		if (methodName.startsWith(COMPUTE_PREFIX) && methodName.length() > COMPUTE_PREFIX.length()) {
			final String field = methodName.substring(COMPUTE_PREFIX.length());
			return Character.toLowerCase(field.charAt(0)) + field.substring(1);
		}
		return null;
	}

	/**
	 * Find direct access to fields in compute methods of function fields.
	 * 
	 */
	private void findComputeDependencies() throws IOException {
		final String className = beanClass.getName().replace('.', '/');
		final ClassReader reader = new ClassReader(ResourceUtils.getResourceStream(className + ".class"));
		final ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5) {

			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				final String field = access == Modifier.PROTECTED ?  getComputedField(name) : null;
				return field == null ? null : new MethodVisitor(Opcodes.ASM5) {

					private Set<String> depends = new LinkedHashSet<>();

					@Override
					public void visitFieldInsn(int opcode, String owner, String name, String desc) {
						if (owner.equals(className) && !name.equals(field)) {
							depends.add(name);
						}
					}

					@Override
					public void visitEnd() {
						computeDependencies.put(field, depends);
					}
				};
			}
		};

		reader.accept(visitor, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
	}

	private Set<String> getComputeDependencies(String property) {
		if (computeDependencies == null) {
			computeDependencies = new HashMap<>();
			try {
				findComputeDependencies();
			} catch (IOException e) {
				return null;
			}
		}
		return computeDependencies.get(property);
	}
	
	private Object interceptCompute(Callable<?> superCall, Method method, Object[] args) throws Exception {
		final String computed = getComputedField(method.getName());
		if (computed == null) {
			return superCall.call();
		}
		final Set<String> depends = getComputeDependencies(computed);
		if (depends == null || depends.isEmpty()) {
			return superCall.call();
		}
		final Mapper mapper = Mapper.of(beanClass);
		method.setAccessible(true);

		if (values.keySet().containsAll(depends)) {
			depends.stream().map(mapper::getProperty).filter(Objects::nonNull).forEach(this::validate);
			return method.invoke(unmanaged(), args);
		}

		// in case of missing fields in context, use intermediate bean
		// but forward all calls to getter to parent proxy
		final Object bean = new ByteBuddy()
				.subclass(beanClass)
				.method(ElementMatchers.isPublic().and(ElementMatchers.isGetter()))
				.intercept(MethodDelegation.to(this))
				.make()
				.load(beanClass.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
				.getLoaded()
				.newInstance();

		depends.stream().map(mapper::getProperty).filter(Objects::nonNull)
			.forEach(p -> {
				validate(p);
				Object source = validated.contains(p.getName()) ? unmanaged() : managed();
				mapper.set(bean, p.getName(), p.get(source));
			});
		return method.invoke(bean, args);
	}

	@RuntimeType
	public Object intercept(@SuperCall Callable<?> superCall, @Origin Method method, @AllArguments Object[] args) throws Throwable {

		// handle compute method calls
		if (Modifier.isProtected(method.getModifiers())) {
			return interceptCompute(superCall, method, args);
		}

		final Property property = findProperty(method.getName());
		// no fields defined or is computed field
		if (property == null || property.isVirtual()) {
			return superCall.call();
		}

		final String fieldName = property.getName();

		// in case of setter, update context map
		if (args.length == 1) {
			values.put(fieldName, args[0]);
		}
	
		// if setter or value found in context map for the getter
		if (args.length == 1 || values.containsKey(fieldName) || property.isTransient()) {
			validate(property);
			return method.invoke(this.unmanaged(), args);
		}
		// else get value from managed instance
		final Object managed = this.managed();
		if (managed == null) {
			return null;
		}

		return method.invoke(managed, args);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T create(final Map<String, Object> values, final Class<T> beanClass) {
		if (values == null) {
			return null;
		}

		final ContextProxy<T> proxy = new ContextProxy<>(values, beanClass);
		final Class<?> proxyClass = new ByteBuddy()
			.subclass(beanClass)
			.method(ElementMatchers.isPublic().and(ElementMatchers.isGetter().or(ElementMatchers.isSetter())))
			.intercept(MethodDelegation.to(proxy))
			.method(ElementMatchers.isProtected().and(ElementMatchers.nameStartsWith(COMPUTE_PREFIX)))
			.intercept(MethodDelegation.to(proxy))
			.implement(ContextEntity.class)
			.method(ElementMatchers.isDeclaredBy(ContextEntity.class))
			.intercept(MethodCall.call(proxy::unmanaged))
			.make()
			.load(beanClass.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
			.getLoaded();

		try {
			return (T) proxyClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
