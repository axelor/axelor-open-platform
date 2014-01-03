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
package com.axelor;

import java.io.InputStreamReader;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.runner.RunWith;

import com.axelor.db.Fixture;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.axelor.test.db.Contact;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(GuiceRunner.class)
@GuiceModules({ MyModule.class })
public abstract class BaseTest {

	@Inject
	protected ObjectMapper objectMapper = new ObjectMapper();

	@Inject
	private Fixture fixture;

	@Before
	public void setUp() {
		if (Contact.all().count() == 0) {
			fixture.load("demo");
		}
	}

	protected InputStreamReader read(String json) {
		return new InputStreamReader(getClass().getClassLoader()
				.getResourceAsStream("META-INF/json/" + json));
	}
	
	protected String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e){
			throw new IllegalArgumentException(e);
		}
	}
	
	protected <T> T fromJson(InputStreamReader reader, Class<T> klass) {
		try {
			return objectMapper.readValue(reader, klass);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	protected <T> T fromJson(String json, Class<T> klass) {
		if (json.endsWith(".js"))
			return fromJson(read(json), klass);
		try {
			return objectMapper.readValue(json, klass);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
}
