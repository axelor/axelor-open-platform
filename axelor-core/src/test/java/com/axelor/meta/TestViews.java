/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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

import java.util.Map;

import javax.persistence.Query;

import org.junit.Assert;
import org.junit.Test;

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

		Title title = Title.all().filter("self.code = ?", "mr").fetchOne();
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
	public void testInclude() throws Exception {
		ObjectViews views = this.unmarshal("com/axelor/meta/Include.xml", ObjectViews.class);

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
