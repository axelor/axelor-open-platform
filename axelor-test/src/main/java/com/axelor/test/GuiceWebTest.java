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
	
	public Client getClient() {
		if (client == null) {
			if (clientConfig == null) {
				throw new RuntimeException("ClientConfig is not injected.");
			}
			client = Client.create(clientConfig);
		}
		return client;
	}

	public WebResource resource() {
		return getClient().resource(BASE_URI);
	}
}
