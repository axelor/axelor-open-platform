/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTest;
import com.axelor.JpaTestModule;
import com.axelor.TestingHelpers;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.FileUtils;
import com.axelor.common.ResourceUtils;
import com.axelor.file.store.s3.S3Cache;
import com.axelor.file.temp.TempFiles;
import com.axelor.test.GuiceModules;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@GuiceModules({S3CacheTest.S3CacheTestModule.class})
public class S3CacheTest extends JpaTest {

  public static class S3CacheTestModule extends JpaTestModule {
    @Override
    protected void configure() {
      resetAllSettings();
      Map<String, String> props = AppSettings.get().getInternalProperties();
      props.put(AvailableAppSettings.DATA_OBJECT_STORAGE_CACHE_ENABLED, "true");
      props.put(AvailableAppSettings.DATA_OBJECT_STORAGE_CACHE_MAX_ENTRIES, "2");
      props.put(AvailableAppSettings.DATA_OBJECT_STORAGE_CACHE_CLEAN_FREQUENCY, "5");
      props.put(AvailableAppSettings.DATA_OBJECT_STORAGE_CACHE_TIME_TO_LIVE, "5");
      super.configure();
    }
  }

  protected static void resetAllSettings() {
    TestingHelpers.resetSettings();
    resetS3Cache();
  }

  protected static void resetS3Cache() {
    try {
      Field instance = S3Cache.class.getDeclaredField("instance");
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

  @BeforeEach
  public void beforeEach() {
    resetS3Cache();
  }

  protected File getTestFile(String name) throws IOException {
    File file = TempFiles.createTempFile().toFile();
    InputStream inputStream = ResourceUtils.getResource("com/axelor/files/" + name).openStream();
    FileUtils.write(file, inputStream);
    return file;
  }

  protected Path getCacheFile(String name) {
    return Paths.get(TempFiles.getRootTempPath().toString(), "s3_cache").resolve(name);
  }

  @Test
  public void addFileTest() throws IOException {
    S3Cache cache = S3Cache.getInstance();
    File file = getTestFile("Logo_Axelor.png");
    cache.put(file, "LogoAxelor.png");
    cache.put(file, "LogoAxelor2.png");

    assertEquals(2, cache.size());

    assertNotNull(cache.get("LogoAxelor.png"));
    assertTrue(Files.exists(getCacheFile("LogoAxelor.png")));
    assertNotNull(cache.get("LogoAxelor2.png"));
    assertTrue(Files.exists(getCacheFile("LogoAxelor2.png")));
  }

  @Test
  public void checkMaxEntriesTest() throws IOException {
    S3Cache cache = S3Cache.getInstance();
    File file = getTestFile("Logo_Axelor.png");
    cache.put(file, "LogoAxelor.png");
    cache.put(file, "LogoAxelor2.png");
    cache.put(file, "LogoAxelor3.png");

    assertEquals(2, cache.size());

    // last used should be evicted and file deleted
    assertNull(cache.get("LogoAxelor.png"));
    assertFalse(Files.exists(getCacheFile("LogoAxelor.png")));
  }

  @Test
  public void checkTTLTest() throws IOException, InterruptedException {
    S3Cache cache = S3Cache.getInstance();
    File file = getTestFile("Logo_Axelor.png");
    cache.put(file, "LogoAxelor.png");

    assertEquals(1, cache.size());

    Thread.sleep(TimeUnit.SECONDS.toMillis(6));

    // file should be expired
    assertNull(cache.get("LogoAxelor.png"));
    assertTrue(cache.isEmpty());
    assertFalse(Files.exists(getCacheFile("LogoAxelor.png")));
  }

  @Test
  public void checkCacheCleanupTest() throws IOException, InterruptedException {
    S3Cache cache = S3Cache.getInstance();
    File file = getTestFile("Logo_Axelor.png");
    cache.put(file, "LogoAxelor.png");

    for (int i = 0; i < 5; i++) {
      cache.get("LogoAxelor.png");
    }

    assertEquals(1, cache.size());

    Thread.sleep(TimeUnit.SECONDS.toMillis(6));

    // file should be expired
    assertNull(cache.get("LogoAxelor.png"));
    assertTrue(cache.isEmpty());
    assertFalse(Files.exists(getCacheFile("LogoAxelor.png")));
  }

  @Test
  public void checkCacheClearTest() throws IOException {
    S3Cache cache = S3Cache.getInstance();
    File file = getTestFile("Logo_Axelor.png");
    cache.put(file, "LogoAxelor.png");
    cache.put(file, "LogoAxelor2.png");
    cache.put(file, "LogoAxelor3.png");

    assertEquals(2, cache.size());
    cache.clear();
    assertTrue(cache.isEmpty());
  }

  @Test
  public void checkRemoveTest() throws IOException {
    S3Cache cache = S3Cache.getInstance();
    File file = getTestFile("Logo_Axelor.png");
    cache.put(file, "LogoAxelor.png");
    cache.get("LogoAxelor.png");
    assertEquals(1, cache.size());
    cache.remove("LogoAxelor.png");
    assertEquals(0, cache.size());
  }
}
