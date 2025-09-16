/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Test;

public class I18nExtractorTest {

  static final String BASE = "..";
  static final String MODULE = "axelor-core";

  @Test
  public void test() {
    I18nExtractor tools = new I18nExtractor();

    Path base = Path.of(BASE, MODULE);
    Path src = base.resolve(Path.of("src", "main"));
    Path dest = base.resolve(Path.of("build", "resources", "test"));

    tools.extract(src, dest, true, true);

    assertTrue(dest.resolve("i18n/messages.csv").toFile().exists());
    assertEquals(3, Objects.requireNonNull(dest.resolve("i18n").toFile().listFiles()).length);
  }
}
