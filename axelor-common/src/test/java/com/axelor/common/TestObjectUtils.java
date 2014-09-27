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
package com.axelor.common;

import java.io.InputStream;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TestObjectUtils {

	@Test
	public void testIsEmpty() {
		Assert.assertTrue(ObjectUtils.isEmpty(null));
		Assert.assertTrue(ObjectUtils.isEmpty(""));
		Assert.assertFalse(ObjectUtils.isEmpty(" "));
		Assert.assertTrue(ObjectUtils.isEmpty(Maps.newHashMap()));
		Assert.assertFalse(ObjectUtils.isEmpty(ImmutableMap.of("some", "value")));
		Assert.assertTrue(ObjectUtils.isEmpty(Lists.newArrayList()));
		Assert.assertFalse(ObjectUtils.isEmpty(Lists.newArrayList("some", "value")));
	}
	
	@Test
	public void testGetResource() {
		URL url = ClassUtils.getResource("log4j.properties");
		Assert.assertNotNull(url);
	}
	
	@Test
	public void testGetResourceStream() {
		InputStream stream = ClassUtils.getResourceStream("log4j.properties");
		Assert.assertNotNull(stream);
	}
}
