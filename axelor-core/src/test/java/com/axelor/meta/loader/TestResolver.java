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
package com.axelor.meta.loader;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TestResolver {

  private Resolver resolver = new Resolver();

  @Test
  public void test() {

    resolver.add("axelor-auth", "axelor-core");
    resolver.add("axelor-meta", "axelor-data");

    resolver.add("axelor-x");

    resolver.add("axelor-sale", "axelor-contact");
    resolver.add("axelor-data", "axelor-auth", "axelor-core");
    resolver.add("axelor-contact", "axelor-auth", "axelor-core", "axelor-meta");
    resolver.add("axelor-project", "axelor-sale");

    List<String> expected =
        Lists.newArrayList(
            "axelor-core",
            "axelor-auth",
            "axelor-data",
            "axelor-meta",
            "axelor-contact",
            "axelor-sale");

    List<String> actual = new ArrayList<>();
    for (Module module : resolver.resolve("axelor-sale")) {
      actual.add(module.getName());
    }

    Assert.assertEquals(expected, actual);

    List<String> all = resolver.names();

    Assert.assertEquals("axelor-core", all.get(0));
    Assert.assertEquals("axelor-project", all.get(all.size() - 1));
  }
}
