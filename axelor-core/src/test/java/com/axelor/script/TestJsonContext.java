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
package com.axelor.script;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.axelor.JpaTest;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.repo.MetaJsonFieldRepository;
import com.axelor.rpc.Context;
import com.axelor.rpc.JsonContext;
import com.axelor.test.db.Contact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.persist.Transactional;

public class TestJsonContext extends JpaTest {

	@Inject
	private ObjectMapper jsonMapper;
	
	@Before
	@Transactional
	public void setup() {
		final MetaJsonFieldRepository repo = Beans.get(MetaJsonFieldRepository.class);
		if (repo.all().filter("self.model = :model AND self.modelField = :field")
				.bind("model", Contact.class.getName()).bind("field", "attrs").count() == 0) {
			
			final Consumer<MetaJsonField> fields = f -> {
				f.setModel(Contact.class.getName());
				f.setModelField("attrs");
				repo.save(f);
			};

			MetaJsonField field;
			
			field = new MetaJsonField();
			field.setName("name");
			field.setType("string");
			fields.accept(field);

			field = new MetaJsonField();
			field.setName("size");
			field.setType("integer");
			fields.accept(field);

			field = new MetaJsonField();
			field.setName("date");
			field.setType("datetime");
			fields.accept(field);

			field = new MetaJsonField();
			field.setName("customer");
			field.setType("many-to-one");
			field.setTargetModel(Contact.class.getName());
			fields.accept(field);
		}
	}

	private String getJson() {
		final Map<String, Object> map = new HashMap<>();

		map.put("name", "Some Name");
		map.put("size", "100");
		
		final Map<String, Object> customer = new HashMap<>();
		customer.put("id", all(Contact.class).fetchOne().getId());
		
		map.put("customer", customer);

		try {
			return jsonMapper.writeValueAsString(map);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	@Test
	public void test() throws Exception {
		final Map<String, Object> values = new HashMap<>();
		final String json = getJson();
		values.put("attrs", json);

		final Context context = new Context(values, Contact.class);
		final ScriptHelper engine = new GroovyScriptHelper(context);

		Assert.assertEquals(json, context.asType(Contact.class).getAttrs());
		Assert.assertTrue(engine.eval("$attrs") instanceof JsonContext);

		Assert.assertTrue(engine.eval("$attrs.customer") instanceof Contact);
		Assert.assertTrue(engine.eval("$attrs.customer.fullName") instanceof String);

		Assert.assertEquals("Some NAME", engine.eval("$attrs.name = 'Some NAME'"));
		Assert.assertEquals(context.get("attrs"), context.asType(Contact.class).getAttrs());
		Assert.assertNotEquals(json, context.asType(Contact.class).getAttrs());
		Assert.assertTrue(context.asType(Contact.class).getAttrs().contains("Some NAME"));

		Assert.assertFalse(context.asType(Contact.class).getAttrs().contains("date"));
		Assert.assertNotNull(engine.eval("$attrs.date = __time__"));
		Assert.assertTrue(context.asType(Contact.class).getAttrs().contains("date"));
	}
}
