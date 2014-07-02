/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.tools.x2j.Extender;
import com.axelor.tools.x2j.Generator;

public class X2JTest {

	InputStream read(String resource) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
	}

	//@Test
	public void testDomains() throws IOException {
		Generator gen = new Generator(
				"src/test/resources/axelor-app/axelor-contact",
				"src/test/resources/axelor-app/axelor-contact/target");
		gen.clean();
		gen.start();
	}

	//@Test
	public void testObjects() throws IOException {

		String base = "src/test/resources/axelor-app";
		String target = "src/test/resources/axelor-app/axelor-objects/target";

		File basePath = new File(base);
		File targetPath = new File(target);

		Extender gen = new Extender(basePath, targetPath, "axelor-objects");
		gen.clean();
		gen.start();
	}
}
