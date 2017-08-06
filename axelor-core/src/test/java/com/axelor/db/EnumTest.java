/*
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
package com.axelor.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Test;

import com.axelor.AbstractTest;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.axelor.test.db.EnumCheck;
import com.axelor.test.db.EnumStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EnumTest extends AbstractTest {

	@Inject
	private ObjectMapper objectMapper;
	
	private Mapper beanMapper = Mapper.of(EnumCheck.class);

	@Test
	public void testMapper() {
		Property status = beanMapper.getProperty("status");
		assertNotNull(status);
		assertEquals(PropertyType.ENUM, status.getType());
		assertEquals(EnumStatus.class, status.getEnumType());
		assertNotNull(status.getEnumItems());
		assertEquals(3, status.getEnumItems().size());
		
		// make sure CLOSED item has custom title
		assertEquals("Close", status.getEnumItems().get(2).get("title"));
		
		EnumCheck entity = new EnumCheck();
		assertNull(entity.getStatus());
		
		status.set(entity, "OPEN");
		assertEquals(EnumStatus.OPEN, entity.getStatus());
		
		status.set(entity, "CLOSED");
		assertEquals(EnumStatus.CLOSED, entity.getStatus());
		
		try {
			status.set(entity, "OPENED");
			fail();
		} catch (IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testJson() throws JsonProcessingException {
		EnumCheck entity = new EnumCheck();
		String json = objectMapper.writeValueAsString(entity);
		assertTrue(json.contains("\"status\":null"));
		entity.setStatus(EnumStatus.OPEN);
		json = objectMapper.writeValueAsString(entity);
		assertTrue(json.contains("\"status\":\"OPEN\""));
	}
}
