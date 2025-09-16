/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.file.store.s3;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.FileUtils;
import com.axelor.file.temp.TempFiles;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A caching when working with S3 as file storage using LRU (Least Recently Used) strategy: the
 * element that hasn't been used for the longest time will be evicted from the cache
 */
public class S3Cache {

  private static final Logger LOG = LoggerFactory.getLogger(S3Cache.class);
  private static final String CACHE_DIR_NAME = "s3_cache";
  public static final boolean CACHE_ENABLED =
      AppSettings.get().getBoolean(AvailableAppSettings.DATA_OBJECT_STORAGE_CACHE_ENABLED, true);

  private static volatile S3Cache instance;

  private final int DEFAULT_TTL;
  private final int CLEAN_FREQUENCY;
  private final int MAX_ENTRIES;
  private volatile AtomicInteger numberOfHit = new AtomicInteger(0);

  Map<String, CacheEntry> cacheEntryMap;
  CacheEntry head;
  CacheEntry tail;
  private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  private S3Cache() {
    final AppSettings settings = AppSettings.get();
    DEFAULT_TTL = settings.getInt(AvailableAppSettings.DATA_OBJECT_STORAGE_CACHE_TIME_TO_LIVE, 600);
    CLEAN_FREQUENCY =
        settings.getInt(AvailableAppSettings.DATA_OBJECT_STORAGE_CACHE_CLEAN_FREQUENCY, 1000);
    MAX_ENTRIES = settings.getInt(AvailableAppSettings.DATA_OBJECT_STORAGE_CACHE_MAX_ENTRIES, 2000);
    cacheEntryMap = new ConcurrentHashMap<>(MAX_ENTRIES);

    try {
      Path cacheDir = getCacheDir();
      if (Files.isDirectory(cacheDir)) {
        FileUtils.deleteDirectory(cacheDir);
      }
    } catch (Exception e) {
      LOG.error("Unable to delete S3 cache directory {} : {}", getCacheDir(), e.getMessage());
    }
  }

  public static S3Cache getInstance() {
    if (instance == null) {
      synchronized (S3Cache.class) {
        if (instance == null) instance = new S3Cache();
      }
    }

    return instance;
  }

  public Path get(String fileName) {
    clean();

    this.lock.readLock().lock();
    try {
      numberOfHit.incrementAndGet();
      CacheEntry item = cacheEntryMap.get(fileName);
      if (item == null) {
        return null;
      }

      Path cacheFile = resolveCachePath(fileName);
      if (!Files.exists(cacheFile) || isExpired(item.lastAccess)) {
        // if the file doesn't exist or is expired, remove it
        deleteFileAndEntry(item);
        return null;
      }

      item.lastAccess = System.currentTimeMillis();
      removeEntry(item);
      addToTail(item);

      return cacheFile;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      this.lock.readLock().unlock();
    }
  }

  public File put(File file, String fileName) {
    try {
      return put(new FileInputStream(file), fileName);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public File put(InputStream inputStream, String fileName) {
    this.lock.writeLock().lock();
    try {

      if (cacheEntryMap.containsKey(fileName)) {
        CacheEntry item = cacheEntryMap.get(fileName);
        item.lastAccess = System.currentTimeMillis();

        removeEntry(item);
        addToTail(item);
      } else {
        // check if exceed max entries
        if (MAX_ENTRIES > -1 && cacheEntryMap.size() >= MAX_ENTRIES) {
          LOG.debug(
              "Maximum number of entries of S3 cache has been reached : {}. Cleaning.",
              MAX_ENTRIES);
          deleteFileAndEntry(head);
        }

        CacheEntry entry = new CacheEntry(fileName, System.currentTimeMillis());
        addToTail(entry);
        cacheEntryMap.put(fileName, entry);
      }

      File tempFile = Path.of(getCacheDir().toString(), fileName).toFile();
      FileUtils.write(tempFile, inputStream);
      return tempFile;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  public boolean remove(String fileName) {
    this.lock.writeLock().lock();
    try {
      CacheEntry entry = cacheEntryMap.get(fileName);
      if (entry == null) {
        return false;
      }
      deleteFileAndEntry(entry);
      return true;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  private boolean isExpired(long lastAccess) {
    return isExpired(lastAccess, System.currentTimeMillis());
  }

  private boolean isExpired(long lastAccess, long time) {
    return DEFAULT_TTL != -1 && lastAccess < time - TimeUnit.SECONDS.toMillis(DEFAULT_TTL);
  }

  private Path resolveCachePath(String fileName) {
    return getCacheDir().resolve(fileName);
  }

  private Path getCacheDir() {
    return Path.of(TempFiles.getRootTempPath().toString(), CACHE_DIR_NAME);
  }

  private void deleteFileAndEntry(CacheEntry entry) throws IOException {
    cacheEntryMap.remove(entry.key);
    Files.deleteIfExists(Path.of(getCacheDir().toString(), entry.key));
    removeEntry(entry);
  }

  private void clean() {
    if (CLEAN_FREQUENCY == -1 || numberOfHit.get() < CLEAN_FREQUENCY) {
      return;
    }
    synchronized (this) {
      if (numberOfHit.get() == 0) {
        return;
      }

      numberOfHit.set(0);
      try {
        LOG.trace("Cleaning S3 cache directory...");
        clean(getCacheDir(), System.currentTimeMillis());
      } catch (IOException e) {
        LOG.error("Error when cleaning S3 cache directory " + getCacheDir(), e);
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
            CacheEntry entry = cacheEntryMap.get(getCacheDir().relativize(file).toString());
            if (isExpired(entry.lastAccess, currentTimeMillis)) {
              deleteFileAndEntry(entry);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path directory, IOException ioException)
              throws IOException {

            if (cacheDir.equals(directory)) {
              return FileVisitResult.CONTINUE;
            }

            try (Stream<Path> stream = Files.list(directory)) {
              if (!stream.iterator().hasNext()) {
                Files.delete(directory);
              }
            }

            return FileVisitResult.CONTINUE;
          }
        });
  }

  private void removeEntry(CacheEntry entry) {

    if (entry.prev != null) {
      entry.prev.next = entry.next;
    } else {
      head = entry.next;
    }

    if (entry.next != null) {
      entry.next.prev = entry.prev;
    } else {
      tail = entry.prev;
    }
  }

  private void addToTail(CacheEntry entry) {

    if (tail != null) {
      tail.next = entry;
    }

    entry.prev = tail;
    entry.next = null;
    tail = entry;

    if (head == null) {
      head = tail;
    }
  }

  public int size() {
    this.lock.readLock().lock();
    try {
      return cacheEntryMap.size();
    } finally {
      this.lock.readLock().unlock();
    }
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public void clear() {
    this.lock.writeLock().lock();
    try {
      cacheEntryMap.forEach(
          (s, cacheEntry) -> {
            try {
              deleteFileAndEntry(cacheEntry);
            } catch (IOException e) {
              // ignore
            }
          });
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  private class CacheEntry {

    private String key;
    private long lastAccess;
    private CacheEntry prev;
    private CacheEntry next;

    public CacheEntry(String key, long lastAccess) {
      this.key = key;
      this.lastAccess = lastAccess;
    }
  }
}
