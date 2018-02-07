/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Test;

import com.axelor.JpaTest;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.axelor.meta.MetaStore;
import com.axelor.meta.schema.views.Selection;
import com.axelor.test.db.EnumCheck;
import com.axelor.test.db.EnumStatus;
import com.axelor.test.db.EnumStatusNumber;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.persist.Transactional;

public class EnumTest extends JpaTest {

	@Inject
	private ObjectMapper objectMapper;

	private Mapper beanMapper = Mapper.of(EnumCheck.class);

	@Override
	public void setUp() {
	}

	@Test
	@Transactional
	public void testPersist() {
		final EntityManager em = getEntityManager();
		final EnumCheck entity = new EnumCheck();
		entity.setStatus(EnumStatus.OPEN);
		entity.setStatusNumber(EnumStatusNumber.THREE);
		em.persist(entity);

		assertNotNull(entity.getId());

		em.flush();
		em.clear();

		final EnumCheck found = em.find(EnumCheck.class, entity.getId());

		assertNotNull(found);
		assertEquals(entity.getStatus(), found.getStatus());
		assertEquals(entity.getStatusNumber(), found.getStatusNumber());
		assertEquals((Integer) 3, found.getStatusNumber().getValue());

		em.clear();

		Query query = em.createNativeQuery("select status from contact_enum_check where id = :id");
		query.setParameter("id", entity.getId());

		List<?> result = query.getResultList();

		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.get(0) instanceof String);
		assertEquals("OPEN", result.get(0));

		query = em.createNativeQuery("select status_number from contact_enum_check where id = :id");
		query.setParameter("id", entity.getId());

		result = query.getResultList();

		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.get(0) instanceof Number);
		assertEquals(3, result.get(0));
	}

	@Test
	public void testMapper() {
		Property status = beanMapper.getProperty("status");
		Property statusNumber = beanMapper.getProperty("statusNumber");
		assertNotNull(status);
		assertNotNull(statusNumber);
		assertEquals(PropertyType.ENUM, status.getType());
		assertEquals(PropertyType.ENUM, statusNumber.getType());
		assertEquals(EnumStatus.class, status.getEnumType());
		assertEquals(EnumStatusNumber.class, statusNumber.getEnumType());

		List<Selection.Option> selectionList = MetaStore.getSelectionList(EnumStatus.class);

		assertNotNull(selectionList);
		assertEquals(3, selectionList.size());

		// make sure CLOSED item has custom title
		assertEquals("Close", selectionList.get(2).getTitle());
		
		selectionList = MetaStore.getSelectionList(EnumStatusNumber.class);

		assertNotNull(selectionList);
		assertEquals(3, selectionList.size());

		// make sure value are stored as extra data
		assertNotNull(selectionList.get(2).getData());
		assertEquals(3, selectionList.get(2).getData().get("value"));

		EnumCheck entity = new EnumCheck();
		assertNull(entity.getStatus());
		assertNull(entity.getStatusNumber());

		status.set(entity, "OPEN");
		assertEquals(EnumStatus.OPEN, entity.getStatus());

		status.set(entity, "CLOSED");
		assertEquals(EnumStatus.CLOSED, entity.getStatus());

		try {
			status.set(entity, "OPENED");
			fail();
		} catch (IllegalArgumentException e) {
		}

		statusNumber.set(entity, 1);
		assertEquals(EnumStatusNumber.ONE, entity.getStatusNumber());

		statusNumber.set(entity, 3);
		assertEquals(EnumStatusNumber.THREE, entity.getStatusNumber());

		try {
			statusNumber.set(entity, 4);
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
		entity.setStatusNumber(EnumStatusNumber.THREE);
		json = objectMapper.writeValueAsString(entity);
		assertTrue(json.contains("\"statusNumber\":\"THREE\""));
		assertTrue(json.contains("\"statusNumber$value\":3"));
	}
}
