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

import org.junit.Assert;
import org.junit.Test;

public class TestClassUtils {

	@Test
	public void testFindClass() {
		Class<?> cls = ClassUtils.findClass("com.axelor.common.StringUtils");
		Assert.assertEquals(cls, StringUtils.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFindClassFail() {
		ClassUtils.findClass("com.axelor.common.StringUtil");
	}
}
