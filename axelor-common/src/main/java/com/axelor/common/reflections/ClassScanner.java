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
package com.axelor.common.reflections;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import com.axelor.internal.asm.AnnotationVisitor;
import com.axelor.internal.asm.ClassReader;
import com.axelor.internal.asm.ClassVisitor;
import com.axelor.internal.asm.Opcodes;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

/**
 * The {@link ClassScanner} uses ASM and guava's ClassPath API to search for
 * types based on super type or annotations.
 * 
 */
final class ClassScanner {

	private static final int ASM_FLAGS = ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES;
	private static final String IGNORE_OBJECT = "java/lang/Object";

	private ClassLoader loader;

	private Map<String, Collector> collectors = new ConcurrentHashMap<>();
	private Set<String> packages = new LinkedHashSet<>();
	private Set<Pattern> pathPatterns = new LinkedHashSet<>();

	/**
	 * Create a new instance of {@link ClassScanner} using the given
	 * {@link ClassLoader}. <br>
	 * <br>
	 * The optional package names can be provided to restrict the scan within
	 * those packages.
	 * 
	 * @param loader
	 *            the {@link ClassLoader} to use for scanning
	 * @param packages
	 *            the package names to restrict the scan within
	 */
	public ClassScanner(ClassLoader loader, String... packages) {
		this.loader = loader;
		if (packages != null) {
			for (String name : packages) {
				this.packages.add(name);
			}
		}
	}
	
	/**
	 * Find with the given URL pattern.
	 *
	 * @param pattern
	 *            the URL pattern
	 * @return the same finder
	 */
	public ClassScanner byURL(String pattern) {
		Objects.requireNonNull(pattern, "pattern must not be null");
		pathPatterns.add(Pattern.compile(pattern));
		return this;
	}

	@SuppressWarnings("all")
	public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
		Set<Class<? extends T>> classes = new HashSet<>();
		Set<String> types;
		try {
			types = getSubTypesOf(type.getName());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		for (String sub : types) {
			try {
				Class<?> found = loader.loadClass(sub);
				classes.add((Class) found);
			} catch (Throwable e) {
			}
		}
		return Collections.unmodifiableSet(classes);
	}

	public Set<Class<?>> getTypesAnnotatedWith(Class<?> annotation) {
		final Set<Class<?>> classes = new HashSet<>();
		if (collectors.isEmpty()) {
			try {
				scan();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		for (String klass : collectors.keySet()) {
			Set<String> my = collectors.get(klass).annotations;
			if (my == null) {
				continue;
			}
			if (my.contains(annotation.getName())) {
				try {
					classes.add(loader.loadClass(klass));
				} catch (Throwable e) {
				}
			}
		}
		return Collections.unmodifiableSet(classes);
	}
	
	private Set<String> getSubTypesOf(String type) throws IOException {
		final Set<String> types = new HashSet<>();
		if (collectors.isEmpty()) {
			scan();
		}

		for (String klass : collectors.keySet()) {
			Set<String> my = collectors.get(klass).superNames;
			if (my == null) {
				continue;
			}
			if (my.contains(type)) {
				types.add(klass);
				types.addAll(getSubTypesOf(klass));
			}
		}
		
		return types;
	}
	
	private void scan() throws IOException {
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Future<?>> futures = new ArrayList<>();
		Set<ClassInfo> infos = new HashSet<>();

		if (packages.isEmpty()) {
			infos = ClassPath.from(loader).getTopLevelClasses();
		} else {
			for (String pkg : packages) {
				infos.addAll(ClassPath.from(loader).getTopLevelClassesRecursive(pkg));
			}
		}
		
		try {
			for (final ClassInfo info : infos) {
				futures.add(executor.submit(() -> {
					scan(info.getName());
					return info.getName();
				}));
			}
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (Exception e) {
				}
			}
		} finally {
			executor.shutdown();
		}
	}

	private void scan(final String type) throws ClassNotFoundException {

		if (collectors.containsKey(type) ||  Object.class.getName().equals(type)) {
			return;
		}

		URL resource = loader.getResource(type.replace('.', '/') + ".class");
		boolean matched = pathPatterns.isEmpty();
		for (Pattern pathPattern : pathPatterns) {
			matched = pathPattern.matcher(resource.getFile()).matches();
			if (matched) break;
		}

		if (!matched) {
			return;
		}

		try {
			InputStream stream = resource.openStream();
			try {
				BufferedInputStream in = new BufferedInputStream(stream);
				ClassReader reader = new ClassReader(in);
				Collector collector = new Collector();
				reader.accept(collector, ASM_FLAGS);
				collectors.put(type, collector);
				if (collector.superNames != null) {
					for (String base : collector.superNames) { scan(base); }
				}
			} finally {
				stream.close();
			}
		} catch (NullPointerException | IOException e) {
			throw new ClassNotFoundException(type);
		}
	}
	
	static class Collector extends ClassVisitor {
		
		private Set<String> superNames;
		private Set<String> annotations;
		
		public Collector() {
			super(Opcodes.ASM4);
		}
		
		private void acceptSuper(String name) {
			if (name == null) {
				return;
			}
			if (superNames == null) {
				superNames = new HashSet<>();
			}
			superNames.add(name.replace("/", "."));
		}
		
		private void acceptAnnotation(String name) {
			if (name == null || IGNORE_OBJECT.equals(name)) {
				return;
			}
			if (annotations == null) {
				annotations = new HashSet<>();
			}
			annotations.add(name.replace("/", ".").substring(1, name.length() - 1));
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			acceptSuper(superName);
			if (interfaces != null) {
				for (String iface : interfaces) {
					acceptSuper(iface);
				}
			}
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			acceptAnnotation(desc);
			return null;
		}
	}
}
