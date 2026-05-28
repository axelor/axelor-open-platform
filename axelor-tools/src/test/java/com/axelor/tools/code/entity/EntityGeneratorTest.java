/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
