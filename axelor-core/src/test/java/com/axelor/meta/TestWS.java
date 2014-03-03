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

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import com.axelor.db.JPA;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.rpc.ActionRequest;
import com.axelor.test.db.Contact;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class TestWS extends MetaTest {
	
	@Inject
	private ActionHandler handler;
	
	private ActionHandler createHandler(String actions, Map<String, Object> context) {
		
		ActionRequest request = new ActionRequest();
		
		Map<String, Object> data = Maps.newHashMap();
		request.setData(data);
		request.setModel("com.axelor.test.db.Contact");
		
		data.put("action", actions);
		data.put("context", context);
		
		return handler.forRequest(request);
	}
	
	@Test
	public void test1() throws Exception {
		ObjectViews views = this.unmarshal("com/axelor/meta/WSTest.xml", ObjectViews.class);
		List<Action> actions = views.getActions();

		Assert.assertNotNull(actions);
		Assert.assertEquals(4, actions.size());
		
		MetaStore.resister(views);
		
		Action action = MetaStore.getAction("data.import.1");
		Map<String, Object> context = Maps.newHashMap();
		
		DateTime dt = new DateTime();
		dt = dt.plus(Period.days(20));
		
		context.put("dt", dt);

		ActionHandler handler = createHandler("data.import.1", context);
		action.evaluate(handler);
	}
	
	@Test
	public void test2() throws Exception {
		ObjectViews views = this.unmarshal("com/axelor/meta/WSTest.xml", ObjectViews.class);

		MetaStore.resister(views);
		
		Action action = MetaStore.getAction("export.sale.order");
		Map<String, Object> context = Maps.newHashMap();
		
		context.put("name", "SO001");
		context.put("orderDate", new LocalDate());
		context.put("customer", ImmutableMap.of("name", "John Smith"));

		List<Object> items = Lists.newArrayList();
		context.put("items", items);
		
		items.add(ImmutableMap.of("product", ImmutableMap.of("name", "PC1"), "price", 250, "quantity", 1));
		items.add(ImmutableMap.of("product", ImmutableMap.of("name", "PC2"), "price", 550, "quantity", 1));
		items.add(ImmutableMap.of("product", ImmutableMap.of("name", "Laptop"), "price", 690, "quantity", 1));

		ActionHandler handler = createHandler("export.sale.order", context);
		action.evaluate(handler);
	}
	
	@Transactional
	public void prepareTest3() {
		final MetaSelect select = new MetaSelect();
		final MetaSelectItem item = new MetaSelectItem();
		
		item.setValue("pizza");
		item.setTitle("Pizza");
		select.addItem(item);
		select.setName("food.selection");

		select.save();
		
		Contact c = new Contact();
		c.setFirstName("John");
		c.setLastName("Smith");
		c.setEmail("john.smith@gmail.com");
		c.setFood("pizza");
		JPA.save(c);
	}

	@Test
	public void test3() throws Exception {
		prepareTest3();
		Contact c = Contact.all().filter("self.email = ?", "john.smith@gmail.com").fetchOne();
		Map<String, Object> context = ImmutableMap.<String, Object>of("person", c);
		
		ActionHandler actionHandler = createHandler("dummy", context);
		
		String text = actionHandler.template("<%__fmt__.debug('Person food : {}', person.food)%>${person.food}");
		Assert.assertEquals("pizza", text);
		
		text = actionHandler.template("${ person.food | text}");
		Assert.assertEquals("Pizza", text);
	}
}
