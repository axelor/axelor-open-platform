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
package com.axelor.tomcat;

import java.nio.file.Path;

import javax.servlet.ServletException;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.FileResourceSet;
import org.apache.catalina.webresources.StandardRoot;

public class TomcatServer {

	private Tomcat tomcat;

	private final TomcatOptions options;

	public TomcatServer(TomcatOptions options) {
		this.options = options;
	}

	public Tomcat create() throws ServletException {
		final Tomcat tomcat = new Tomcat();
		tomcat.setBaseDir(options.getBaseDir().toFile().getAbsolutePath());

		final int port = options.getPort();
		final String contextPath = options.getContextPath();
		final String docBase = options.getDocBase().toFile().getAbsolutePath();

		final Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setPort(port);

		tomcat.setConnector(connector);
		tomcat.setPort(port);

		final StandardContext context = (StandardContext) tomcat.addWebapp(contextPath, docBase);
		final StandardRoot resources = new StandardRoot();

		context.setResources(resources);
		context.setUnpackWAR(false);

		// additional webapp resources
		options.getExtraResources()
			.stream()
			.map(Path::toFile)
			.map(dir -> new DirResourceSet(resources, "/", dir.getAbsolutePath(), "/"))
			.forEach(resources::addPostResources);

		// additional classes, should be search before libs
		options.getClasses()
			.stream()
			.map(Path::toFile)
			.map(dir -> new DirResourceSet(resources, "/WEB-INF/classes", dir.getAbsolutePath(), "/"))
			.forEach(resources::addPreResources);

		// additional libs
		options.getLibs()
			.stream()
			.map(Path::toFile)
			.map(file -> new FileResourceSet(resources, "/WEB-INF/lib/" + file.getName(), file.getAbsolutePath(), "/"))
			.forEach(resources::addPostResources);

		tomcat.getServer().addLifecycleListener(event -> {
			final Lifecycle lifecycle = event.getLifecycle();
			switch (lifecycle.getState()) {
			case FAILED:
				System.err.println("Context [" + contextPath + "] failed in [" + lifecycle.getClass().getName() + "] lifecycle.");
				break;
			case STARTED:
				System.err.println("Running at http://localhost:" + port + contextPath);
				break;
			default:
				break;
			}
		});

		Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

		return tomcat;
	}

	public void start() {
		try {
			if (tomcat == null) {
				tomcat = create();
			}
			tomcat.start();
		} catch (Exception e) {
			throw new RuntimeException("Cannot start Tomcat " + e.getMessage(), e);
		}
		tomcat.getServer().await();
	}

	public void stop() {
		if (tomcat == null) {
			return;
		}
		try {
			tomcat.stop();
		} catch (Exception e) {
			throw new RuntimeException("Cannot Stop Tomcat " + e.getMessage(), e);
		}
	}
}
