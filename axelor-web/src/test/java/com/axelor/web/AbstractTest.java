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
