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
package com.axelor.web;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceWebTest;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;

@GuiceModules(WebTestModule.class)
public abstract class AbstractTest extends GuiceWebTest {
	
	@BeforeClass
	public static void setUp() {
		startServer();
	}
	
	@AfterClass
	public static void tearDown(){
		stopServer();
	}
	
	protected WebResource.Builder jsonPath(String path, String... params) {
		
		MultivaluedMap<String, String> form = new Form();
		
		for(int i=0; i < params.length; i += 2) {
			String param = params[i], value = "";
			try {
				value = params[i+1];
			} catch (IndexOutOfBoundsException e){}
			form.add(param, value);
		}

		return resource().path(path).queryParams(form)
				.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON);
	}

}
