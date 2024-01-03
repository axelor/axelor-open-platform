/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.tools.code.entity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

public class EntityGeneratorTest {

  InputStream read(String resource) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
  }

  @Test
  public void testDomains() throws IOException {

    File domainPath = new File("src/test/resources/domains");
    File outputPath = new File("build/src-gen");

    EntityGenerator gen = new EntityGenerator(domainPath, outputPath);

    // add lookup source
    domainPath = new File("src/test/resources/search");
    EntityGenerator lookup = new EntityGenerator(domainPath, outputPath);

    gen.addLookupSource(lookup);

    gen.start();
  }
}
