/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

public class I18nExtractorTest {

  static final String BASE = "..";
  static final String MODULE = "axelor-core";

  @Test
  public void test() throws IOException {
    I18nExtractor tools = new I18nExtractor();

    Path base = Path.of(BASE, MODULE);
    Path src = base.resolve(Path.of("src", "main"));
    Path dest = base.resolve(Path.of("build", "resources", "test"));

    tools.extract(src, dest, true, true);

    Path messagesFile = dest.resolve("i18n/messages.csv");
    assertTrue(Files.isRegularFile(messagesFile));
    assertEquals(3, Objects.requireNonNull(dest.resolve("i18n").toFile().listFiles()).length);

    String messagesContent = Files.readString(messagesFile);
    Matcher matcher = Pattern.compile("(?<!\r)\n").matcher(messagesContent);
    assertFalse(matcher.find(), "Should uniformly use CRLF for line endings");
  }
}
