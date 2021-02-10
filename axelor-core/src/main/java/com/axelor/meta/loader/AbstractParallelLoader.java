/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AbstractParallelLoader extends AbstractLoader {

  protected abstract List<List<URL>> findFileLists(Module module);

  protected abstract void doLoad(URL file, Module module, boolean update);

  @Override
  protected void doLoad(Module module, boolean update) {
    findFileLists(module).stream()
        .flatMap(List::stream)
        .forEach(file -> doLoad(file, module, update));
  }

  protected void feedTransactionExecutor(
      ParallelTransactionExecutor transactionExecutor,
      Module module,
      boolean update,
      Set<Path> paths) {

    for (final ListIterator<List<URL>> it = findFileLists(module, paths).listIterator();
        it.hasNext(); ) {
      final int priority = it.nextIndex();
      final List<URL> files = it.next();
      files
          .parallelStream()
          .forEach(file -> transactionExecutor.add(() -> doLoad(file, module, update), priority));
    }
  }

  private List<List<URL>> findFileLists(Module module, Set<Path> paths) {
    final List<List<URL>> lists = findFileLists(module);

    // All files
    if (paths.isEmpty()) {
      return lists;
    }

    // Filtered by modified in JAR if any
    final List<List<URL>> foundInJar = findFileListsJar(lists);

    if (foundInJar.parallelStream().anyMatch(list -> !list.isEmpty())) {
      return foundInJar;
    }

    // Filtered by paths
    return lists
        .parallelStream()
        .map(
            list ->
                list.parallelStream()
                    .filter(
                        url ->
                            "file".equals(url.getProtocol())
                                && paths.contains(Paths.get(toUri(url))))
                    .collect(Collectors.toList()))
        .collect(Collectors.toList());
  }

  private List<List<URL>> findFileListsJar(List<List<URL>> lists) {
    return lists.stream()
        .flatMap(List::stream)
        .findAny()
        .filter(url -> "jar".equals(url.getProtocol()))
        .map(
            url -> {
              try (final FileSystem fs =
                  FileSystems.newFileSystem(toUri(url), Collections.emptyMap())) {
                return lists
                    .parallelStream()
                    .map(
                        list ->
                            list.parallelStream()
                                .filter(
                                    urlInFs ->
                                        getLastModifiedTimeMillis(fs, urlInFs)
                                            >= ModuleManager.getLastRestored())
                                .collect(Collectors.toList()))
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
    final Path path = fs.getPath(Paths.get(toUri(url)).toString());
    try {
      return Files.getLastModifiedTime(path).toMillis();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
