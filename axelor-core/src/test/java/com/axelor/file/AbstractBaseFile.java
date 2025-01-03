/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
import static org.junit.jupiter.api.Assertions.fail;

import com.axelor.JpaTest;
import com.axelor.JpaTestModule;
import com.axelor.TestingHelpers;
import com.axelor.common.ResourceUtils;
import com.axelor.file.store.FileStoreFactory;
import com.axelor.file.store.Store;
import com.axelor.file.store.UploadedFile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

public abstract class AbstractBaseFile extends JpaTest {

  public static class FileStoreTestModule extends JpaTestModule {
    @Override
    protected void configure() {
      resetAllSettings();
      super.configure();
    }
  }

  protected static void resetAllSettings() {
    TestingHelpers.resetSettings();
    try {
      Field instance = FileStoreFactory.class.getDeclaredField("_store");
      instance.setAccessible(true);
      instance.set(null, null);
    } catch (Exception e) {
      throw new RuntimeException();
    }
  }

  @AfterAll
  public static void clear() {
    resetAllSettings();
  }

  @Test
  @Order(10)
  public void addFileTest() throws IOException {
    Store store = FileStoreFactory.getStore();

    UploadedFile uploadedFile = store.addFile(getResource("Logo_Axelor.png"), "Logo_Axelor.png");

    assertTrue(store.hasFile("Logo_Axelor.png"));
    assertEquals("Logo_Axelor.png", uploadedFile.getName());
    assertEquals("Logo_Axelor.png", uploadedFile.getPath());

    uploadedFile = store.addFile(getResource("Logo_Axelor.png"), "logo/logo2.png");

    assertTrue(store.hasFile("logo/logo2.png"));
    assertEquals("logo2.png", uploadedFile.getName());
    assertEquals("logo/logo2.png", uploadedFile.getPath());
  }

  @Test
  @Order(20)
  public void getFileTest() throws IOException {
    Store store = FileStoreFactory.getStore();

    File file = store.getFile("Logo_Axelor.png");

    assertTrue(file.exists());
    assertTrue(
        IOUtils.contentEquals(file.toURI().toURL().openStream(), getResource("Logo_Axelor.png")));
  }

  @Test
  @Order(30)
  public void getStreamTest() throws IOException {
    Store store = FileStoreFactory.getStore();

    InputStream inputStream = store.getStream("Logo_Axelor.png");

    assertTrue(IOUtils.contentEquals(inputStream, getResource("Logo_Axelor.png")));
  }

  @Test
  @Order(40)
  public void addFileSubDirTest() throws IOException {
    Store store = FileStoreFactory.getStore();

    InputStream resource = getResource("Logo_Axelor.png");
    store.addFile(resource, "subDir/Logo_Axelor.png");

    assertTrue(store.hasFile("subDir/Logo_Axelor.png"));
  }

  @Test
  @Order(50)
  public void deleteFileTest() {
    Store store = FileStoreFactory.getStore();

    store.deleteFile("Logo_Axelor.png");

    assertFalse(store.hasFile("Logo_Axelor.png"));
  }

  @Test
  @Order(60)
  public void getGhostFile() {
    Store store = FileStoreFactory.getStore();

    try {
      store.getFile("ghostFile.png");
      fail("File shouldn't exist");
    } catch (Exception e) {
      // ignore
    }
  }

  protected InputStream getResource(String name) throws IOException {
    return ResourceUtils.getResource("com/axelor/files/" + name).openStream();
  }
}
