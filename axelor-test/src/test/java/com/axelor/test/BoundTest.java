/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
package com.axelor.test;

import static org.junit.Assert.assertEquals;

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
