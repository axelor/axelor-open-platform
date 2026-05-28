/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.loader;

import com.axelor.db.ParallelTransactionExecutor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AbstractParallelLoader extends AbstractLoader {

  protected abstract List<URL> findFiles(Module module);

  protected abstract void doLoad(URL file, Module module, boolean update);

  @Override
  protected void doLoad(Module module, boolean update) {
    findFiles(module).forEach(file -> doLoad(file, module, update));
  }

  protected void feedTransactionExecutor(
      ParallelTransactionExecutor transactionExecutor,
      Module module,
      boolean update,
      Set<Path> paths) {

    for (URL file : findFiles(module, paths)) {
      transactionExecutor.add(() -> doLoad(file, module, update));
    }
  }

  protected List<URL> findFiles(Module module, Set<Path> paths) {
    final List<URL> lists = findFiles(module);

    // All files
    if (paths.isEmpty()) {
      return lists;
    }

    // Filtered by modified in JAR if any
    final List<URL> foundInJar = findFilesInJar(lists);

    if (!foundInJar.isEmpty()) {
      return foundInJar;
    }

    // Filtered by paths
    return lists.parallelStream()
        .filter(url -> "file".equals(url.getProtocol()) && paths.contains(Path.of(toUri(url))))
        .collect(Collectors.toList());
  }

  private List<URL> findFilesInJar(List<URL> lists) {
    return lists.stream()
        .findAny()
        .filter(url -> "jar".equals(url.getProtocol()))
        .map(
            url -> {
              try (final FileSystem fs =
                  FileSystems.newFileSystem(toUri(url), Collections.emptyMap())) {
                return lists.parallelStream()
                    .filter(
                        urlInFs ->
                            getLastModifiedTimeMillis(fs, urlInFs)
                                >= ModuleManager.getLastRestored())
                    .collect(Collectors.toList());
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            })
        .orElse(Collections.emptyList());
  }

  private URI toUri(URL url) {
    try {
      return url.toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private long getLastModifiedTimeMillis(FileSystem fs, URL url) {
    final Path path = fs.getPath(Path.of(toUri(url)).toString());
    try {
      return Files.getLastModifiedTime(path).toMillis();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
