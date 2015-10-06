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
package com.axelor.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.views.FormView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class TestActions extends MetaTest {
	
	private ObjectViews views;
	
	@Inject
	private ContactRepository contacts;

	@Before
	public void setUp() {
		try {
			views = this.unmarshal("com/axelor/meta/Contact.xml", ObjectViews.class);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
		
		assertNotNull(views);
		assertNotNull(views.getActions());
		
		MetaStore.resister(views);
		ensureContact();
	}

	@Transactional
	void ensureContact() {
		if (contacts.all().count() == 0) {
			Contact c = new Contact();
			c.setFirstName("John");
			c.setLastName("Smith");
			c.setEmail("john.smith@gmail.com");
			contacts.save(c);
		}
	}

	private ActionHandler createHandler(String action, Map<String, Object> context) {
		
		ActionRequest request = new ActionRequest();
		
		Map<String, Object> data = Maps.newHashMap();
		request.setData(data);
		request.setModel("com.axelor.test.db.Contact");
		request.setAction(action);

		data.put("context", context);
		
		return new ActionHandler(request);
	}
	
	private ActionHandler createHandler(Action action, Map<String, Object> context) {
		Preconditions.checkArgument(action != null, "action is null");
		return createHandler(action.getName(), context);
	}
	
	@Test
	public void testRecord() {
		
		Action action = MetaStore.getAction("action-contact-defaults");
		ActionHandler handler = createHandler(action, null);
		
		Object value = action.evaluate(handler);
		assertTrue(value instanceof Contact);
		
		Contact c = (Contact) value;

		assertNotNull(c.getTitle());
		assertEquals("Mr. John Smith", c.getFullName());
		
		//System.err.println("XXX: " + c);
	}
	
	@Test
	public void testMultiRecord() {
		
		Action action = MetaStore.getAction("action-contact-defaults-multi");
		ActionHandler handler = createHandler(action, null);
		
		Object value = action.evaluate(handler);
		assertTrue(value instanceof Contact);
		
		Contact c = (Contact) value;

		assertNotNull(c.getLastName());
		assertNotNull(c.getFirstName());
		assertEquals(c.getFirstName(), c.getLastName());
		assertEquals("Smith", c.getLastName());
		assertEquals("Mr. Smith Smith", c.getFullName());
		
		assertNotNull(c.getEmail());
		assertNotNull(c.getProEmail());
		assertEquals(c.getProEmail(), c.getEmail());
		assertEquals("john.smith@gmail.com", c.getEmail());
	}

	@Test
	@SuppressWarnings("all")
	public void testAttrs() {
		Action action = MetaStore.getAction("action-contact-attrs");
		ActionHandler handler = createHandler(action, null);
		
		Object value = action.evaluate(handler);
		assertTrue(value instanceof Map);
		
		Map<String, Object> map = (Map) value;
		Map<String, Object> attrs = (Map) map.get("lastName");
		
		assertTrue(attrs instanceof Map);
		assertEquals(true, attrs.get("readonly"));
		assertEquals(true, attrs.get("hidden"));
		
		attrs = (Map) map.get("notes");
		
		assertTrue(attrs instanceof Map);
	}
	
	@Test
	@SuppressWarnings("all")
	public void testAttrsMutli() {
		Action action = MetaStore.getAction("action-contact-attrs-multi");
		ActionHandler handler = createHandler(action, null);
		
		Object value = action.evaluate(handler);
		assertTrue(value instanceof Map);
		
		Map<String, Object> map = (Map) value;
		Map<String, Object> attrs = (Map) map.get("lastName");
		
		assertTrue(attrs instanceof Map);
		assertEquals(true, attrs.get("readonly"));
		assertEquals(true, attrs.get("hidden"));
		
		attrs = (Map) map.get("notes");
		
		assertTrue(attrs instanceof Map);
		assertEquals("About Me", attrs.get("title"));
		
		Map<String, Object> attrsPhone = (Map) map.get("phone");
		Map<String, Object> attrsNotes = (Map) map.get("notes");
		Map<String, Object> attrsBirth = (Map) map.get("dateOfBirth");
		
		assertTrue(attrs instanceof Map);
		assertEquals(true, attrsPhone.get("hidden"));
		assertEquals(attrsPhone.get("hidden"), attrsNotes.get("hidden"));
		assertEquals(attrsBirth.get("hidden"), attrsNotes.get("hidden"));
		
		Map<String, Object> attrsFisrtName = (Map) map.get("firstName");
		Map<String, Object> attrsLastName = (Map) map.get("lastName");
		
		assertTrue(attrs instanceof Map);
		assertEquals(true, attrsFisrtName.get("readonly"));
		assertEquals(attrsFisrtName.get("readonly"), attrsLastName.get("readonly"));
		assertEquals(true, attrsLastName.get("hidden"));
	}

	@Test
	public void testValidate() {
		
		Action action = MetaStore.getAction("action-contact-validate");
		Map<String, Object> context = Maps.newHashMap();
		
		context.put("id", 1);
		context.put("firstName", "John");
		context.put("lastName", "Sm");
		
		ActionHandler handler = createHandler(action, context);
		Object value = action.evaluate(handler);
		
		assertNotNull(value);
	}
	
	@Test
	@SuppressWarnings("all")
	public void testCondition() {
		
		Action action = MetaStore.getAction("check.dates");
		Map<String, Object> context = Maps.newHashMap();
		
		context.put("orderDate", new LocalDate("2012-12-10"));
		context.put("createDate", new LocalDate("2012-12-11"));
		
		ActionHandler handler = createHandler(action, context);
		Object value = action.evaluate(handler);
		
		assertNotNull(value);
		assertTrue(value instanceof Map);
		assertTrue(!((Map) value).isEmpty());
	}
	
	@Test
	@SuppressWarnings("all")
	public void testMethod() {
		
		Action action = MetaStore.getAction("action-contact-greetings");
		Map<String, Object> context = Maps.newHashMap();
		
		context.put("id", 1);
		context.put("firstName", "John");
		context.put("lastName", "Smith");
		
		ActionHandler handler = createHandler(action, context);
		Object value = action.evaluate(handler);
		
		assertNotNull(value);
		assertEquals("Hello World!!!", ((Map)((List<?>)((ActionResponse)value).getData()).get(0)).get("flash") );
		
	}
	
	@Test
	public void testRpc() {
		
		Action action = MetaStore.getAction("action-contact-greetings-rpc");
		Map<String, Object> context = Maps.newHashMap();
		
		context.put("id", 1);
		context.put("firstName", "John");
		context.put("lastName", "Smith");
		context.put("fullName", "John Smith");
		
		ActionHandler handler = createHandler(action, context);
		Object value = action.evaluate(handler);
		
		assertNotNull(value);
		assertEquals("Say: John Smith", value);
		
		value = handler.evaluate("call: com.axelor.meta.web.Hello:say(fullName)");
		
		assertNotNull(value);
		assertEquals("Say: John Smith", value);
	}
	
	@Test
	public void testEvents() throws Exception {
		
		FormView view = (FormView) MetaStore.getView("contact-form");
		assertNotNull(view);
		
		Map<String, Object> context = Maps.newHashMap();
		
		context.put("firstName", "John");
		context.put("lastName", "Smith");
		
		String onLoad = view.getOnLoad();
		String onSave = view.getOnSave();
		
		ActionHandler handler = createHandler(onLoad, context);
		ActionResponse response = handler.execute();
		System.err.println(response.getData());
		
		handler = createHandler(onSave, context);
		response = handler.execute();
		System.err.println(response.getData());
	}
	
 	@Test
	public void testView() {
		
		Action action = MetaStore.getAction("action-view-contact");
		Map<String, Object> context = Maps.newHashMap();
		
		context.put("id", 1);
		context.put("firstName", "John");
		context.put("lastName", "Smith");
		
		ActionHandler handler = createHandler(action, context);
		Object value = action.evaluate(handler);
		
		assertNotNull(value);
	}
 	
 	@Test
 	public void testGroup() {
 		Action action = MetaStore.getAction("action.group.test");
		Map<String, Object> context = Maps.newHashMap();
		
		context.put("id", 1);
		context.put("firstName", "John");
		context.put("lastName", "Smith");
		
		ActionHandler handler = createHandler(action, context);
		Object value = action.evaluate(handler);
		
		Assert.assertNotNull(value);
		Assert.assertTrue(value instanceof List);
		Assert.assertFalse(((List<?>)value).isEmpty());
		Assert.assertNotNull(((List<?>)value).get(0));
		Assert.assertFalse(value.toString().contains("pending"));
		
		handler.getContext().update("firstName", "J");
		handler.getContext().update("email", "j.smith@gmail.com");
		
		value = action.evaluate(handler);
		
		Assert.assertNotNull(value);
		Assert.assertTrue(value instanceof List);
		Assert.assertFalse(((List<?>)value).isEmpty());
		Assert.assertNotNull(((List<?>)value).get(0));
		Assert.assertTrue(value.toString().contains("pending"));
 	}

}
