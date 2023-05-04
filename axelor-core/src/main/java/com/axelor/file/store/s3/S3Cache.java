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
package com.axelor.file.store.s3;

import com.axelor.common.FileUtils;
import com.axelor.file.temp.TempFiles;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Cache {

  private static final Logger LOG = LoggerFactory.getLogger(S3Cache.class);

  private static volatile S3Cache instance;

  // time to live in seconds
  private static final int DEFAULT_TTL = 5 * 60;

  // number of hit to clean the cache
  private static final int CLEAN_FREQUENCY = 200;

  private int numberOfHit = 0;

  private static final String CACHE_DIR_NAME = "s3_cache";

  private S3Cache() {
    try {
      Files.deleteIfExists(getCacheDir());
    } catch (Exception e) {
      LOG.error("Unable to delete s3 cache directory " + getCacheDir());
    }
  }

  public File get(String fileName) {
    clean();
    File cacheFile = resolveCachePath(fileName).toFile();
    if (cacheFile.exists() && isExpired(cacheFile)) {
      return cacheFile;
    }
    return null;
  }

  public File put(File file, String fileName) {
    try {
      File tempFile = Paths.get(getCacheDir().toString(), fileName).toFile();
      FileUtils.copyFile(file, tempFile);
      return tempFile;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public File put(InputStream inputStream, String fileName) {
    try {
      File tempFile = Paths.get(getCacheDir().toString(), fileName).toFile();
      FileUtils.write(tempFile, inputStream);
      return tempFile;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isExpired(File cacheFile) {
    return isExpired(cacheFile.toPath(), System.currentTimeMillis());
  }

  private boolean isExpired(Path cachePath, long time) {
    try {
      return Files.getLastModifiedTime(cachePath).toMillis()
              + TimeUnit.SECONDS.toMillis(DEFAULT_TTL)
          > time;
    } catch (IOException e) {
      return true;
    }
  }

  private Path resolveCachePath(String fileName) {
    return getCacheDir().resolve(fileName);
  }

  public static S3Cache getInstance() {
    if (instance == null) {
      synchronized (S3Cache.class) {
        if (instance == null) instance = new S3Cache();
      }
    }

    return instance;
  }

  private Path getCacheDir() {
    return Paths.get(TempFiles.getRootTempPath().toString(), CACHE_DIR_NAME);
  }

  private void clean() {
    numberOfHit++;
    if (numberOfHit < CLEAN_FREQUENCY) {
      return;
    }
    synchronized (this) {
      if (numberOfHit == 0) {
        return;
      }

      numberOfHit = 0;
      try {
        clean(getCacheDir(), System.currentTimeMillis());
      } catch (IOException e) {
        LOG.error("Error when cleaning s3 cache directory " + getCacheDir(), e);
      }
    }
  }

  private void clean(Path cacheDir, long currentTimeMillis) throws IOException {
    if (!Files.isDirectory(cacheDir)) {
      return;
    }
    Files.walkFileTree(
        cacheDir,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            if (isExpired(file, currentTimeMillis)) {
              Files.deleteIfExists(file);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path directory, IOException ioException)
              throws IOException {

            try (Stream<Path> stream = Files.list(directory)) {
              if (!stream.iterator().hasNext()) {
                Files.delete(directory);
              }
            }

            return FileVisitResult.CONTINUE;
          }
        });
  }
}
