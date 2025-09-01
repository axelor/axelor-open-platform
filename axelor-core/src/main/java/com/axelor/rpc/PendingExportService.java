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
