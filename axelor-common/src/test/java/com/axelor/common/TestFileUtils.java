/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class TestFileUtils {

  @Test
  public void testGetFile() {

    File file = FileUtils.getFile("file.text");
    assertEquals("file.text", file.getPath());

    file = FileUtils.getFile("my", "dir", "file.text");
    assertEquals("my/dir/file.text".replace("/", File.separator), file.getPath());
  }

  @Test
  public void testDirUtils() {

    File source = new File("src");
    File target = new File("bin/src-copy");

    try {
      FileUtils.copyDirectory(source, target);
    } catch (IOException e) {
      fail();
    }

    assertTrue(target.exists() && target.isDirectory());
    assertNotNull(target.listFiles());
    assertTrue(target.listFiles().length > 0);

    try {
      FileUtils.deleteDirectory(target);
    } catch (IOException e) {
      fail();
    }

    assertFalse(target.exists());
  }

  @Test
  public void testSafeFileName() {
    assertNull(FileUtils.safeFileName(null));
    assertEquals(FileUtils.safeFileName(""), "");
    
    //trim
    assertEquals("toto.txt", FileUtils.safeFileName(" toto.txt "));

    //accent
    assertEquals("Cesar.txt", FileUtils.safeFileName("César.txt"));

    // illegal
    assertEquals("", FileUtils.safeFileName("?[]/\\=<>:;,'\"&$#*()|~`!{',}%+’«»”“"));

    // space
    assertEquals("a-fil-e.txt", FileUtils.safeFileName("a fil e.txt"));

    // illegal start and end chars
    assertEquals("a-file.txt", FileUtils.safeFileName("-a file.txt"));
    assertEquals("a-file.txt", FileUtils.safeFileName(".a file.txt."));

    // legal
    assertEquals("toto.txt", FileUtils.safeFileName("toto.txt"));
    assertEquals("漢字.txt", FileUtils.safeFileName("漢字.txt"));
  }
}
