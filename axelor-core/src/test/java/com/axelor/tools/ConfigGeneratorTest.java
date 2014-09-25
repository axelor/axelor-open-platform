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
package com.axelor.tools;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import com.axelor.AbstractTest;

public class ConfigGeneratorTest extends AbstractTest {

	@Test
	public void test1() throws IOException {
		ConfigGenerator gen = new ConfigGenerator();

		StringWriter writer = new StringWriter();
		gen.generate(writer);
	}

	@Test
	public void test2() throws IOException {
		ConfigGenerator gen = new ConfigGenerator();
		gen.setBigName("big");

		StringWriter writer = new StringWriter();
		gen.ehcache(writer);
	}
}
