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
package com.axelor.meta;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.axelor.JpaTest;
import com.axelor.common.ResourceUtils;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class MetaTest extends JpaTest {

	@Inject
	private ObjectMapper mapper;

	protected InputStream read(String resource) {
		return ResourceUtils.getResourceStream(resource);
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
