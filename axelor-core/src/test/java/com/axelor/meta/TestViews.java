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
