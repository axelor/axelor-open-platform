/*
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

import com.axelor.common.VersionUtils.Version;

public class TestVersionUtils {

	@Test
	public void test() {

		Version v1 = new Version("3.0.1");
		Version v2 = new Version("3.0.1-rc1");
		Version v3 = new Version("3.0.1-SNAPSHOT");

		Assert.assertEquals("3.0", v1.feature);
		Assert.assertEquals("3.0", v2.feature);
		Assert.assertEquals("3.0", v3.feature);

		Assert.assertEquals(3, v1.major);
		Assert.assertEquals(0, v1.minor);
		Assert.assertEquals(1, v1.patch);
		Assert.assertEquals(0, v1.rc);

		Assert.assertEquals(3, v2.major);
		Assert.assertEquals(0, v2.minor);
		Assert.assertEquals(1, v2.patch);
		Assert.assertEquals(1, v2.rc);

		Assert.assertEquals(3, v3.major);
		Assert.assertEquals(0, v3.minor);
		Assert.assertEquals(1, v3.patch);
		Assert.assertEquals(0, v3.rc);

		Assert.assertTrue(v1.matches("3.0.1"));
		Assert.assertFalse(v1.matches("3.0.0"));
		Assert.assertFalse(v1.matches("3.0.2"));
		Assert.assertTrue(v1.matches("~3.0.0"));
		Assert.assertTrue(v1.matches("~3.0.1"));
		Assert.assertFalse(v1.matches("~3.0.2"));
	}
}
