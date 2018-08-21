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
package com.axelor.common;

import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

public class TestFileUtils {

  @Test
  public void testGetFile() {

    File file = FileUtils.getFile("file.text");
    Assert.assertEquals("file.text", file.getPath());

    file = FileUtils.getFile("my", "dir", "file.text");
    Assert.assertEquals("my/dir/file.text".replace("/", File.separator), file.getPath());
  }

  @Test
  public void testDirUtils() {

    File source = new File("src");
    File target = new File("bin/src-copy");

    try {
      FileUtils.copyDirectory(source, target);
    } catch (IOException e) {
      Assert.fail();
    }

    Assert.assertTrue(target.exists() && target.isDirectory());
    Assert.assertNotNull(target.listFiles());
    Assert.assertTrue(target.listFiles().length > 0);

    try {
      FileUtils.deleteDirectory(target);
    } catch (IOException e) {
      Assert.fail();
    }

    Assert.assertFalse(target.exists());
  }
}
