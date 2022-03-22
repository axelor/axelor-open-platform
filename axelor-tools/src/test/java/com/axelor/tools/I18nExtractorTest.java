/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.tools.i18n.I18nExtractor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.junit.jupiter.api.Test;

public class I18nExtractorTest {

  static final String BASE = "..";
  static final String MODULE = "axelor-core";

  @Test
  public void test() {
    I18nExtractor tools = new I18nExtractor();

    Path base = Paths.get(BASE, MODULE);
    Path src = base.resolve(Paths.get("src", "main"));
    Path dest = base.resolve(Paths.get("build", "resources", "test"));

    tools.extract(src, dest, true, true);

    assertTrue(dest.resolve("i18n/messages.csv").toFile().exists());
    assertEquals(3, Objects.requireNonNull(dest.resolve("i18n").toFile().listFiles()).length);
  }
}
