package com.axelor.meta;

import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;

import com.axelor.meta.views.Action;
import com.axelor.meta.views.ObjectViews;
import com.axelor.rpc.ActionRequest;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class TestWS extends AbstractTest {
	
	@Inject
	private Injector injector;
	
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
	public void test1() throws Exception {
		ObjectViews views = this.unmarshal("WSTest.xml", ObjectViews.class);
		List<Action> actions = views.getActions();

		Assert.assertNotNull(actions);
		Assert.assertEquals(3, actions.size());
		
		MetaStore.resister(views);
		
		Action action = MetaStore.getAction("data.test.1");
		Map<String, Object> context = Maps.newHashMap();
		
		DateTime dt = new DateTime();
		dt = dt.plus(Period.days(20));
		
		context.put("dt", dt);

		ActionHandler handler = createHandler("data.test.1", context);
		action.evaluate(handler);
	}
}
