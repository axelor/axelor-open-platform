/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
import java.io.Closeable;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

abstract class AbstractParallelLoader extends AbstractLoader {

  protected abstract List<List<URL>> findFileLists(Module module);

  protected abstract void doLoad(URL file, Module module, boolean update);

  @Override
  protected void doLoad(Module module, boolean update) {
    findFileLists(module)
        .stream()
        .flatMap(List::stream)
        .forEach(file -> doLoad(file, module, update));
  }

  protected void feedTransactionExecutor(
      ParallelTransactionExecutor transactionExecutor,
      Module module,
      boolean update,
      Set<Path> paths) {
    final Function<Module, List<List<URL>>> findFileListsFunc;

    if (paths.isEmpty()) {
      findFileListsFunc = this::findFileLists;
    } else if (paths.iterator().next().toString().endsWith(".jar")) {
      findFileListsFunc = this::findFileListsJar;
    } else {
      findFileListsFunc = m -> findFileListsPath(m, paths);
    }

    for (final ListIterator<List<URL>> it = findFileListsFunc.apply(module).listIterator();
        it.hasNext(); ) {
      final int priority = it.nextIndex();
      final List<URL> files = it.next();
      files
          .parallelStream()
          .forEach(file -> transactionExecutor.add(() -> doLoad(file, module, update), priority));
    }
  }

  private List<List<URL>> findFileListsJar(Module module) {
    final List<List<URL>> lists = findFileLists(module);
    final Optional<URL> firstURLOpt = lists.stream().flatMap(List::stream).findFirst();

    if (!firstURLOpt.isPresent()) {
      return Collections.emptyList();
    }

    final URL firstURL = firstURLOpt.get();

    try (final JarFileSystem jarFS = new JarFileSystem(firstURL.toURI())) {
      return lists
          .parallelStream()
          .map(
              list ->
                  list.parallelStream()
                      .filter(
                          url -> {
                            try {
                              return Files.getLastModifiedTime(jarFS.getPath(url.toURI()))
                                      .toMillis()
                                  >= ModuleManager.getLastRestored();
                            } catch (IOException e) {
                              throw new UncheckedIOException(e);
                            } catch (URISyntaxException e) {
                              throw new RuntimeException(e);
                            }
                          })
                      .collect(Collectors.toList()))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private List<List<URL>> findFileListsPath(Module module, Set<Path> paths) {
    return findFileLists(module)
        .parallelStream()
        .map(
            list ->
                list.parallelStream()
                    .filter(
                        url -> {
                          try {
                            return paths.contains(Paths.get(url.toURI()));
                          } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                          }
                        })
                    .collect(Collectors.toList()))
        .collect(Collectors.toList());
  }

  private static class JarFileSystem implements Closeable {
    private final FileSystem jarFS;

    public JarFileSystem(URI uri) throws IOException {
      jarFS = FileSystems.newFileSystem(uri, Collections.emptyMap());
    }

    public Path getPath(URI uri) {
      return jarFS.getPath(Paths.get(uri).toString());
    }

    @Override
    public void close() throws IOException {
      jarFS.close();
    }
  }
}
