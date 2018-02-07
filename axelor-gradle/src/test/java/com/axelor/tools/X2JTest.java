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
package com.axelor.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.axelor.tools.x2j.Generator;

public class X2JTest {

	InputStream read(String resource) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
	}

	@Test
	public void testDomains() throws IOException {

		File domainPath = new File("src/test/resources/domains");
		File outputPath = new File("build/src-gen");

		Generator gen = new Generator(domainPath, outputPath);
		
		// add lookup source
		domainPath = new File("src/test/resources/search");
		Generator lookup = new Generator(domainPath, outputPath);
		
		gen.addLookupSource(lookup);

		gen.clean();
		gen.start();
	}
}
