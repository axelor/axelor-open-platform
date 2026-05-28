/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
