/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.test;

import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.WebappContext;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;

@RunWith(GuiceWebRunner.class)
public abstract class GuiceWebTest extends GuiceTest {

	private static Logger LOG = LoggerFactory.getLogger(GuiceWebTest.class);
	
	private static URI BASE_URI =  UriBuilder.fromUri("http://localhost:9997/test").build();

	private static HttpServer server;
	
	private static GuiceFilter filter;
	
	@Inject
	private ClientConfig clientConfig;
	
	private Client client;
	
	/**
	 * This method should be called to start the embedded grizzly server. <br>
	 * <br>
	 * For example:<br>
	 * 
	 * <pre>
	 * &#064;GuiceModules(MyModule.class)
	 * public class MyTest extends GuiceWebTest {
	 * 
	 * 	&#064;BeforeClass
	 * 	public static void beforeClass() {
	 * 		GuiceWebTest.startServer();
	 * 	}
	 * }
	 * </pre>
	 */
	public static void startServer() {
		
		if (server != null) {
			return;
		}

		LOG.info("Creating grizzly...");

		WebappContext context = new WebappContext("TestContext", BASE_URI.getRawPath());
		context.addFilter("guiceFilter", filter = new GuiceFilter())
			   .addMappingForUrlPatterns(null, "/*");
		
		context.addServlet("TestServlet", new HttpServlet() {
			private static final long serialVersionUID = 1L;
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
			}
		}).addMapping("/test/*");

		try {
			server = GrizzlyServerFactory.createHttpServer(BASE_URI, (HttpHandler) null);
			context.deploy(server);
			server.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * This method should be called to stop the embedded grizzly server. <br>
	 * <br>
	 * For example:<br>
	 * 
	 * <pre>
	 * &#064;GuiceModules(MyModule.class)
	 * public class MyTest extends GuiceWebTest {
	 * 
	 * 	&#064;AfterClass
	 * 	public static void afterClass() {
	 * 		GuiceWebTest.stopServer();
	 * 	}
	 * }
	 * </pre>
	 */
	public static void stopServer() {
		try {
			LOG.info("Stopping grizzly...");
			if (server != null) {
				filter.destroy();
				server.stop();
				server = null;
			}
		} catch (Exception e) {
			LOG.warn("Could not stop grizzly...", e);
		}
	}
	
	/**
	 * Get an instance of {@link Client} created from an injected {@link ClientConfig}.
	 * 
	 */
	public Client getClient() {
		if (client == null) {
			if (clientConfig == null) {
				throw new RuntimeException("ClientConfig is not injected.");
			}
			client = Client.create(clientConfig);
		}
		return client;
	}

	/**
	 * Get a {@link WebResource} connected to the embbedded web server at http://localhost:9997/test url.
	 * 
	 */
	public WebResource resource() {
		return getClient().resource(BASE_URI);
	}
}
