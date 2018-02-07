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
package com.axelor.meta.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.Query;

import org.junit.Assert;
import org.junit.Test;

import com.axelor.common.ResourceUtils;
import com.axelor.meta.MetaTest;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.FormInclude;
import com.axelor.meta.schema.views.FormView;
import com.axelor.meta.schema.views.Search;
import com.axelor.script.ScriptHelper;
import com.axelor.test.db.Title;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

public class TestViews extends MetaTest {
	
	@Inject
	private ViewLoader loader;

	@Test
	public void test1() throws Exception {
		ObjectViews views = this.unmarshal("com/axelor/meta/Contact.xml", ObjectViews.class);

		assertNotNull(views);
		assertNotNull(views.getViews());
		assertEquals(2, views.getViews().size());

		String json = toJson(views);

		assertNotNull(json);
	}

	@Test
	public void test2() throws Exception {
		ObjectViews views = this.unmarshal("com/axelor/meta/Welcome.xml", ObjectViews.class);

		assertNotNull(views);
		assertNotNull(views.getViews());
		assertEquals(1, views.getViews().size());

		String json = toJson(views);

		assertNotNull(json);
	}

	@Test
	@Transactional
	public void test3() throws Exception {
		ObjectViews views = this.unmarshal("com/axelor/meta/Search.xml", ObjectViews.class);
		assertNotNull(views);
		assertNotNull(views.getViews());
		assertEquals(1, views.getViews().size());

		String json = toJson(views);

		assertNotNull(json);

		Search search = (Search) views.getViews().get(0);

		Title title = all(Title.class).filter("self.code = ?", "mr").fetchOne();
		Assert.assertNotNull(title);

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
	@Transactional
	public void testInclude() throws Exception {
		
		try (InputStream is = ResourceUtils.getResourceStream("com/axelor/meta/Include.xml")) {
			loader.process(is, new Module("test"), false);

			final AbstractView form1 = XMLViews.findView("contact-form1", null, null, "test");
			final AbstractView form2 = XMLViews.findView("contact-form2", null, null, "test");

			assertTrue(form1 instanceof FormView);
			assertTrue(form2 instanceof FormView);

			final FormInclude include = (FormInclude) ((FormView) form2).getItems().get(0);
			final AbstractView included = include.getView();

			assertEquals(form1.getName(), included.getName());
		}
	}
}
