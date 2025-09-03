/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc;

import com.axelor.cache.CacheBuilder;
import com.axelor.cache.event.RemovalCause;
import com.axelor.common.StringUtils;
import com.axelor.file.temp.TempFiles;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
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

  private static final Map<String, String> pendingExports =
      CacheBuilder.newBuilder("pendingExports")
          .expireAfterWrite(Duration.ofMinutes(5))
          .removalListener(
              (String token, String filePath, RemovalCause cause) -> {
                // Delete if it has expired (unconsumed export)
                if (cause == RemovalCause.EXPIRED) {
                  try {
                    Files.deleteIfExists(Path.of(filePath));
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
   * <p>Creates a temporary file from the given stream and returns the token associated with it.
   *
   * @param stream the stream to the export file
   * @return the token associated with the pending export file
   */
  public String add(InputStream stream) {
    Path tempFile;

    try {
      tempFile = TempFiles.createTempFile("pending-", null);
      Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    var token = UUID.randomUUID().toString();
    var tempFilePath = tempFile.normalize().toAbsolutePath().toString();

    if (pendingExports.putIfAbsent(token, tempFilePath) != null) {
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

  private @Nullable Path filterRegularFile(@Nullable String filePath) {
    if (StringUtils.isBlank(filePath)) {
      return null;
    }

    var path = Path.of(filePath);
    return Files.isRegularFile(path) ? path : null;
  }
}
