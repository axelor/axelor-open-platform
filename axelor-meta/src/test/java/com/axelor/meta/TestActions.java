package com.axelor.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import com.axelor.db.JPA;
import com.axelor.meta.db.Contact;
import com.axelor.meta.views.Action;
import com.axelor.meta.views.FormView;
import com.axelor.meta.views.ObjectViews;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class TestActions extends AbstractTest {
	
	private ObjectViews views;
	
	@Inject
	private Injector injector;
	
	@Before
	public void setUp() throws Exception {

		views = this.unmarshal("Contact.xml", ObjectViews.class);
		assertNotNull(views);
		assertNotNull(views.getActions());
		
		MetaStore.resister(views);
		
		if (Contact.all().count() == 0) {
			JPA.runInTransaction(new Runnable() {
				
				@Override
				public void run() {
					Contact c = new Contact();
					c.setFirstName("John");
					c.setLastName("Smith");
					c.setEmail("john.smith@gmail.com");
					JPA.save(c);
				}
			});
		}
	}
	
	private ActionHandler createHandler(String actions, Map<String, Object> context) {
		
		ActionRequest request = new ActionRequest();
		
		Map<String, Object> data = Maps.newHashMap();
		request.setData(data);
		request.setModel("com.axelor.meta.db.Contact");
		
		data.put("action", actions);
		data.put("context", context);
		
		return new ActionHandler(request, injector);
	}
	
	@Test
	public void testRecord() {
		
		Action action = MetaStore.getAction("action-contact-defaults");
		ActionHandler handler = createHandler("action-contact-defaults", null);
		
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
		ActionHandler handler = createHandler("action-contact-defaults-multi", null);
		
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

	@Test @SuppressWarnings("all")
	public void testAttrs() {
		Action action = MetaStore.getAction("action-contact-attrs");
		ActionHandler handler = createHandler("action-contact-attrs", null);
		
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
	
	@Test @SuppressWarnings("all")
	public void testAttrsMutli() {
		Action action = MetaStore.getAction("action-contact-attrs-multi");
		ActionHandler handler = createHandler("action-contact-attrs-multi", null);
		
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
		
		ActionHandler handler = createHandler("action-contact-validate", context);
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
		
		ActionHandler handler = createHandler("check.dates", context);
		Object value = action.evaluate(handler);
		
		assertNotNull(value);
		assertTrue(value instanceof Map);
		assertTrue(!((Map) value).isEmpty());
		
		Map<String, String> error = (Map<String, String>) value;
		
		assertEquals("Order create date is in future.", error.get("createDate"));
	}
	
	@Test @SuppressWarnings("all")
	public void testMethod() {
		
		Action action = MetaStore.getAction("action-contact-greetings");
		Map<String, Object> context = Maps.newHashMap();
		
		context.put("id", 1);
		context.put("firstName", "John");
		context.put("lastName", "Smith");
		
		ActionHandler handler = createHandler("action-contact-greetings", context);
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
		
		ActionHandler handler = createHandler("action-contact-greetings-rpc", context);
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
		
		ActionHandler handler = createHandler("action-view-contact", context);
		Object value = action.evaluate(handler);
		
		assertNotNull(value);
	}
 	
 	@Test
	public void testTest() {
		
 		Action actionTest = MetaStore.getAction("test.action");
 		assertNotNull(actionTest);
 		
		Action action = MetaStore.getAction("action-contact-test");
		Map<String, Object> context = Maps.newHashMap();
		
		context.put("id", 1);
		context.put("firstName", "John");
		context.put("lastName", "Smith");
		
		ActionHandler handler = createHandler("action-contact-test", context);
		Object value = action.evaluate(handler);
		Contact c = (Contact) value;
		
		assertEquals("john.smith@gmail.com", c.getEmail());
		assertEquals("john.smith@gmail.fr", c.getProEmail());
		assertEquals("john.smith", c.getFirstName());
	}

}
