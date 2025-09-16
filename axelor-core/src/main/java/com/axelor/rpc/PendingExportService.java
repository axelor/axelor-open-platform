/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc;

import com.axelor.cache.CacheBuilder;
import com.axelor.cache.event.RemovalCause;
import com.axelor.file.temp.TempFiles;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Manages pending export files.
 *
 * <p>Pending export files expire after some period of time and are automatically deleted if not
 * consumed.
 */
@Singleton
public class PendingExportService {

  private static final Map<String, Path> pendingExports =
      CacheBuilder.newBuilder("pendingExports")
          .expireAfterWrite(Duration.ofMinutes(5))
          .removalListener(
              (String token, Path path, RemovalCause cause) -> {
                // Delete if it has expired (unconsumed export)
                if (cause == RemovalCause.EXPIRED) {
                  try {
                    Files.deleteIfExists(path);
                  } catch (IOException e) {
                    throw new UncheckedIOException(e);
                  }
                }
              })
          .build()
          .asMap();

  /**
   * Adds a pending export file.
   *
   * @param path the path to the export file
   * @return the token associated with the pending export file
   */
  public String add(Path path) {
    var fullPath = path.isAbsolute() ? path : TempFiles.getTempPath().resolve(path);

    Path tempFile;
    try {
      tempFile = TempFiles.createTempFile("pending-", null);
      Files.copy(fullPath, tempFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    var token = UUID.randomUUID().toString();

    if (pendingExports.putIfAbsent(token, tempFile) != null) {
      // Should never happen.
      throw new IllegalStateException("Duplicate token: " + token);
    }

    return token;
  }

  /**
   * Returns the pending export file by its token.
   *
   * @param token the token associated with the pending export file
   * @return the pending export file or null
   */
  public @Nullable Path get(String token) {
    return filterRegularFile(pendingExports.get(token));
  }

  /**
   * Removes and returns the pending export file by its token.
   *
   * <p>It is your responsibility to delete the export file after you're done with it.
   *
   * @param token the token associated with the pending export file
   * @return the pending export file or null
   */
  public @Nullable Path remove(String token) {
    return filterRegularFile(pendingExports.remove(token));
  }

  private @Nullable Path filterRegularFile(Path path) {
    return Files.isRegularFile(path) ? path : null;
  }
}
