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
package com.axelor.common;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.axelor.common.bar.MyBase;
import com.axelor.common.reflections.Reflections;

@SuppressWarnings("all")
public class TestReflections implements Serializable {

	@Test
	public void testClassFinder() {

		Set<?> all;

		// scan by sub type
		all = Reflections
				.findSubTypesOf(Map.class)
				.find();

		Assert.assertNotNull(all);
		Assert.assertTrue(all.size() > 2);

		// scan by annotation within a package
		all = Reflections
				.findTypes()
				.having(Ignore.class)
				.within("com.axelor.common")
				.find();

		Assert.assertNotNull(all);
		Assert.assertEquals(2, all.size());

		// scan by annotation within a package
		all = Reflections
				.findTypes()
				.having(Ignore.class)
				.within("com.axelor.common.foo")
				.find();

		Assert.assertNotNull(all);
		Assert.assertEquals(1, all.size());

		// scan by sub type and annotation
		all = Reflections
				.findSubTypesOf(MyBase.class)
				.having(Ignore.class)
				.within("com.axelor")
				.find();

		// scan by url pattern
		all = Reflections
				.findSubTypesOf(Map.class)
				.byURL(".*/axelor-common/.*")
				.find();

		Assert.assertNotNull(all);
		Assert.assertEquals(4, all.size());
	}
	
	@Test
	public void testResourceFinder() {
		Assert.assertNotNull(Reflections
				.findResources()
				.byName("(.*)\\.java")
				.find());
	}
}
