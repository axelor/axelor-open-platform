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

import java.time.LocalDateTime;
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
import com.axelor.meta.db.MetaJsonModel;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.meta.db.repo.MetaJsonFieldRepository;
import com.axelor.meta.db.repo.MetaJsonModelRepository;
import com.axelor.meta.db.repo.MetaJsonRecordRepository;
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
		
		final MetaJsonModelRepository jsonModels = Beans.get(MetaJsonModelRepository.class);
		final MetaJsonFieldRepository jsonFields = Beans.get(MetaJsonFieldRepository.class);

		if (jsonFields.all().filter("self.model = :model AND self.modelField = :field")
				.bind("model", Contact.class.getName()).bind("field", "attrs").count() == 0) {
			
			final Consumer<MetaJsonField> fields = f -> {
				f.setModel(Contact.class.getName());
				f.setModelField("attrs");
				jsonFields.save(f);
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
		
		if (jsonModels.findByName("hello") == null) {
			final MetaJsonModel hello = new MetaJsonModel();
			hello.setName("hello");
			hello.setTitle("Hello");
			hello.addField(new MetaJsonField() {{
				setName("name");
				setNameField(true);
			}});
			hello.addField(new MetaJsonField() {{
				setName("date");
				setType("datetime");
			}});
			
			jsonModels.save(hello);

			final MetaJsonModel world = new MetaJsonModel();
			world.setName("world");
			world.setTitle("World");
			world.addField(new MetaJsonField() {{
				setName("name");
				setNameField(true);
			}});
			world.addField(new MetaJsonField() {{
				setName("price");
				setType("decimal");
			}});
			
			hello.addField(new MetaJsonField() {{
				setName("world");
				setType("custom-many-to-one");
				setTargetJsonModel(world);
			}});

			jsonModels.save(world);
		}
	}

	private String getCustomerAttrsJson() {
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
	public void testCustomFields() throws Exception {
		final Map<String, Object> values = new HashMap<>();
		final String json = getCustomerAttrsJson();
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
		
		context.put("customer", null);
		Assert.assertFalse(context.asType(Contact.class).getAttrs().contains("customer"));

		context.put("customer", all(Contact.class).fetchOne());
		Assert.assertTrue(context.asType(Contact.class).getAttrs().contains("customer"));

		try {
			context.put("customer", new Contact());
			Assert.fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	@Transactional
	public void testCustomModels() throws Exception {
		final MetaJsonRecordRepository $json = Beans.get(MetaJsonRecordRepository.class);

		final Context helloCtx = $json.create("hello");
		helloCtx.put("name", "Hello!!!");
		helloCtx.put("date", LocalDateTime.now());
		
		final Context worldCtx = $json.create("world");
		worldCtx.put("name", "World!!!");
		worldCtx.put("price", 1000.25);

		final MetaJsonRecord world = $json.save(worldCtx);
		
		helloCtx.put("world", world);

		final MetaJsonRecord hello = $json.save(helloCtx);
		
		Assert.assertNotNull(hello.getAttrs());
		Assert.assertNotNull(world.getAttrs());
		Assert.assertEquals("Hello!!!", hello.getName());
		Assert.assertEquals("World!!!", world.getName());

		Assert.assertTrue(hello.getAttrs().contains("date"));
		Assert.assertTrue(hello.getAttrs().contains("world"));
		Assert.assertTrue(hello.getAttrs().contains("World!!!"));
		Assert.assertTrue(world.getAttrs().contains("1000.25"));

		final Context ctx = $json.create(hello);
		Assert.assertEquals("Hello!!!", ctx.get("name"));
		Assert.assertTrue(ctx.get("world") instanceof Map);
	}
}
