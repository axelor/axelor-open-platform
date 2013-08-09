package com.axelor.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import javax.persistence.Query;

import org.junit.Assert;
import org.junit.Test;

import com.axelor.db.JPA;
import com.axelor.meta.db.Title;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.FormInclude;
import com.axelor.meta.schema.views.FormView;
import com.axelor.meta.schema.views.Search;
import com.axelor.meta.script.ScriptHelper;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

public class TestViews extends AbstractTest {

	@Test
	public void test1() throws Exception {
		ObjectViews views = this.unmarshal("Contact.xml", ObjectViews.class);

		assertNotNull(views);
		assertNotNull(views.getViews());
		assertEquals(2, views.getViews().size());

		String json = toJson(views);

		assertNotNull(json);
	}

	@Test
	public void test2() throws Exception {
		ObjectViews views = this.unmarshal("Welcome.xml", ObjectViews.class);

		assertNotNull(views);
		assertNotNull(views.getViews());
		assertEquals(1, views.getViews().size());

		String json = toJson(views);

		assertNotNull(json);
	}

	@Test
	@Transactional
	public void test3() throws Exception {
		ObjectViews views = this.unmarshal("Search.xml", ObjectViews.class);
		assertNotNull(views);
		assertNotNull(views.getViews());
		assertEquals(1, views.getViews().size());

		String json = toJson(views);

		assertNotNull(json);

		Search search = (Search) views.getViews().get(0);

		Title title = new Title();
		title.setCode("mr");
		title.setName("Mr.");
		title = JPA.save(title);

		Map<String, Object> binding = Maps.newHashMap();
		binding.put("customer", "Some");
		binding.put("date", "2011-11-11");
		binding.put("xxx", 111);
		binding.put("title", title);
		binding.put("country", "IN");
		binding.put("value", "100.10");

		Map<String, Object> partner = Maps.newHashMap();
		partner.put("firstName", "Name");

		binding.put("partner", partner);

		ScriptHelper helper = search.scriptHandler(binding);

		for(Search.SearchSelect s : search.getSelects()) {
			Query q = s.toQuery(search, helper);
			if (q == null)
				continue;
			q.setFirstResult(0);
			q.setMaxResults(search.getLimit());

			assertNotNull(q.getResultList());
		}
	}

	@Test
	public void testInclude() throws Exception {
		ObjectViews views = this.unmarshal("Include.xml", ObjectViews.class);

		MetaStore.resister(views);

		assertNotNull(views);
		assertNotNull(views.getViews());
		assertEquals(2, views.getViews().size());

		FormView view = (FormView) views.getViews().get(1);

		assertNotNull(view.getItems());
		assertEquals(2, view.getItems().size());

		FormInclude include = (FormInclude) view.getItems().get(0);

		AbstractView included = include.getView();

		assertNotNull(included);
		Assert.assertTrue(included instanceof FormView);

		String json = toJson(include);

		assertNotNull(json);
	}
}
