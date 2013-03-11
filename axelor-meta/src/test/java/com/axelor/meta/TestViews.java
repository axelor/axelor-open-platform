package com.axelor.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import javax.persistence.Query;

import org.junit.Test;

import com.axelor.db.JPA;
import com.axelor.meta.db.Title;
import com.axelor.meta.views.ObjectViews;
import com.axelor.meta.views.Search;
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
		
		GroovyScriptHelper helper = search.scriptHandler(binding);
		
		for(Search.SearchSelect s : search.getSelects()) {
			Query q = s.toQuery(search, helper);
			if (q == null)
				continue;
			q.setFirstResult(0);
			q.setMaxResults(search.getLimit());
			
			assertNotNull(q.getResultList());
		}
	}
}
