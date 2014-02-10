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
package com.axelor.meta;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.axelor.MyTest;
import com.axelor.common.ClassUtils;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class MetaTest extends MyTest {

	@Inject
	private ObjectMapper mapper;

	protected InputStream read(String resource) {
		return ClassUtils.getResourceStream(resource);
	}

	protected ObjectMapper getObjectMapper() {
		return mapper;
	}

	@SuppressWarnings("unchecked")
	protected <T> T unmarshal(String resource, Class<T> type) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(type);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		return (T) unmarshaller.unmarshal(read(resource));
	}

	protected String toJson(Object object) throws JsonGenerationException,
			JsonMappingException, IOException {
		return getObjectMapper()
				.writerWithDefaultPrettyPrinter()
				.writeValueAsString(object);
	}
}
