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
package com.axelor.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.file.temp.TempFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class TempFileTest {

  @Test
  public void createTempFile() throws IOException {
    Path tempFile = TempFiles.createTempFile();
    assertTrue(Files.exists(tempFile));
    assertTrue(tempFile.toString().endsWith(".tmp"));
    assertEquals(tempFile.getParent(), TempFiles.getTempPath());

    tempFile = TempFiles.createTempFile(null, null);
    assertTrue(Files.exists(tempFile));
    assertTrue(tempFile.toString().endsWith(".tmp"));
    assertEquals(tempFile.getParent(), TempFiles.getTempPath());

    tempFile = TempFiles.createTempFile(null, "");
    assertTrue(Files.exists(tempFile));
    assertFalse(tempFile.toString().endsWith(".tmp"));
    assertEquals(tempFile.getParent(), TempFiles.getTempPath());

    tempFile = TempFiles.createTempFile("test", "");
    assertTrue(Files.exists(tempFile));
    assertTrue(TempFiles.getTempPath().relativize(tempFile).toString().startsWith("test"));
    assertEquals(tempFile.getParent(), TempFiles.getTempPath());
  }

  @Test
  public void createTempDir() throws IOException {
    Path tempFile = TempFiles.createTempDir();
    assertTrue(Files.exists(tempFile));
    assertEquals(tempFile.getParent(), TempFiles.getTempPath());

    tempFile = TempFiles.createTempDir(null);
    assertTrue(Files.exists(tempFile));
    assertEquals(tempFile.getParent(), TempFiles.getTempPath());

    tempFile = TempFiles.createTempDir("test");
    assertTrue(Files.exists(tempFile));
    assertTrue(TempFiles.getTempPath().relativize(tempFile).toString().startsWith("test"));
    assertEquals(tempFile.getParent(), TempFiles.getTempPath());
  }

  @Test
  public void findTempFile() throws IOException {
    Path tempFile = TempFiles.findTempFile(String.valueOf(UUID.randomUUID()));
    assertFalse(Files.exists(tempFile));

    tempFile = TempFiles.createTempFile("my", ".csv");
    tempFile = TempFiles.findTempFile(tempFile.getFileName().toString());
    assertTrue(Files.exists(tempFile));
  }
}
