/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.app.internal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * This class is used to launch app from IDE.
 * 
 * <p>
 * It will locate <code>axelor-tomcat.jar</code> and launch application using
 * isolated class loader with hotswap-agent support.
 * </p>
 * 
 */
public final class AppRunner {

	private static class FilteringClassLoader extends URLClassLoader {

		private static final ClassLoader EXT_LOADER = ClassLoader.getSystemClassLoader().getParent();

		private static final String[] ALLOWED_PACKAGES = { "org.hotswap" };

		public FilteringClassLoader(ClassLoader parent) {
			super(new URL[0], parent);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			try {
				return EXT_LOADER.loadClass(name);
			} catch (ClassNotFoundException ignore) {
			}

			if (!classAllowed(name)) {
				throw new ClassNotFoundException(name);
			}

			Class<?> cl = super.loadClass(name, false);
			if (resolve) {
				resolveClass(cl);
			}

			return cl;
		}

		private boolean classAllowed(String className) {
			for (String prefix : ALLOWED_PACKAGES) {
				if (className.startsWith(prefix)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public URL getResource(String name) {
			return EXT_LOADER.getResource(name);
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			return EXT_LOADER.getResources(name);
		}
	}

	private static void run(String[] args) {
		final Path runnerJar = Paths.get("build", "tomcat", "axelor-tomcat.jar");
		final List<URL> urls = new ArrayList<>();

		try (JarFile jar = new JarFile(runnerJar.toFile())) {
			String cp = jar.getManifest().getMainAttributes().getValue("Class-Path");
			for (String s : cp.split(" ")) {
				urls.add(Paths.get(s.trim()).toUri().toURL());
			}
		} catch (IOException e) {
			System.err.println("invalid jar: " + runnerJar);
			System.err.println("please run 'runnerJar' grade task");
			return;
		}

		ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
		ClassLoader parentClassLoader = systemClassLoader.getParent();

		for (URL url : ((URLClassLoader) systemClassLoader).getURLs()) {
			if (url.toString().contains("hotswap-agent")) {
				urls.add(url);
				parentClassLoader = new FilteringClassLoader(systemClassLoader);
				break;
			}
		}

		try (URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[] {}), parentClassLoader)) {
			Thread.currentThread().setContextClassLoader(loader);
			Class<?> runnerClass = loader.loadClass("com.axelor.tomcat.TomcatRunner");
			Method runnerMain = runnerClass.getMethod("main", String[].class);
			runnerMain.invoke(null, (Object) args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean isTestPath(URL url) {
		try {
			Path path = Paths.get(url.toURI());
			if (!Files.isDirectory(path)) {
				return false;
			}
			while (path != null && !Files.exists(path.resolve("build.gradle"))) {
				if (path.getFileName().toString().equals("test")) {
					return true;
				}
				path = path.getParent();
			}
		} catch (URISyntaxException e) {
			// ignore
		}
		return false;
	}

	public static void main(String[] args) {
		
		final URLClassLoader systemLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		final List<URL> urls = Arrays.stream(systemLoader.getURLs())
				.filter(u -> !isTestPath(u)) // remove test paths (eclipse may add them)
				.collect(Collectors.toList());

		try (URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[] {}), null)) {
			Thread.currentThread().setContextClassLoader(loader);
			Properties props = new Properties();

			props.load(loader.getResourceAsStream("application.properties"));
			Class<?> loggerClass = loader.loadClass("com.axelor.common.logging.LoggerConfiguration");
			Object logger = loggerClass.getConstructor(Properties.class).newInstance(props);
			Method install = loggerClass.getDeclaredMethod("install");
			Method uninstall = loggerClass.getDeclaredMethod("uninstall");
			install.invoke(logger);
			try {
				run(args);
			} finally {
				uninstall.invoke(logger);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Thread.currentThread().setContextClassLoader(systemLoader);
		}
	}
}
