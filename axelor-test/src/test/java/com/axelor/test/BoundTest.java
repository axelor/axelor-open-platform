/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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

import static junit.framework.Assert.assertEquals;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.servlet.RequestScoped;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

@GuiceModules(BoundTest.Module.class)
public class BoundTest extends GuiceWebTest {

	static class Module extends AbstractModule {

		@Override
		protected void configure() {
			bind(BoundPerRequestResource.class);
			bind(BoundNoScopeResource.class);
			bind(BoundSingletonResource.class);
			
			bind(ClientConfig.class).to(DefaultClientConfig.class);
		}
	}
	
	@Path("bound/perrequest")
    @RequestScoped
    public static class BoundPerRequestResource {

        @Context UriInfo ui;
        
        @QueryParam("x") String x;
        
        @GET
        @Produces("text/plain")
        public String getIt() {
            assertEquals("bound/perrequest", ui.getPath());
            assertEquals("x", x);
            
            return "OK";
        }
    }
	
	@Path("bound/noscope")
    public static class BoundNoScopeResource {

        @Context UriInfo ui;

        @QueryParam("x") String x;

        @GET
        @Produces("text/plain")
        public String getIt() {
            assertEquals("bound/noscope", ui.getPath());
            assertEquals("x", x);

            return "OK";
        }
    }

    @Path("bound/singleton")
    @Singleton
    public static class BoundSingletonResource {

        @Context UriInfo ui;

        @GET
        @Produces("text/plain")
        public String getIt() {
            assertEquals("bound/singleton", ui.getPath());
            String x = ui.getQueryParameters().getFirst("x");
            assertEquals("x", x);

            return "OK";
        }
    }
    
    @BeforeClass
    public static void beforeClass() {
    	GuiceWebTest.startServer();
    }

    @Test
	public void testBoundPerRequestScope() {
		WebResource r = resource().path("/bound/perrequest").queryParam("x", "x");
        String s = r.get(String.class);
        assertEquals(s, "OK");
	}
	
	@Test
	public void testBoundNoScopeResource() {
        WebResource r = resource().path("/bound/noscope").queryParam("x", "x");
        String s = r.get(String.class);
        assertEquals(s, "OK");
    }

	@Test
    public void testBoundSingletonResourcee() {
        WebResource r = resource().path("/bound/singleton").queryParam("x", "x");
        String s = r.get(String.class);
        assertEquals(s, "OK");
    }
}
