/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.meta.loader;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import com.axelor.common.reflections.Reflections;
import com.axelor.i18n.I18nBundle;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaStore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.persist.UnitOfWork;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ViewWatcher {

  private static final Logger LOG = LoggerFactory.getLogger(ViewWatcher.class);

  private static ViewWatcher instance;
  private static ModuleManager moduleManager;
  private static Set<String> moduleNames;
  private static String appName;

  private WatchService watcher;
  private final Map<WatchKey, Path> keys = new HashMap<>();

  private Set<String> pendingModules;
  private Set<Path> pendingPaths;
  private ScheduledExecutorService scheduler;
  private ScheduledFuture<?> scheduledFuture;

  private boolean running;

  private static final long UPDATE_DELAY =
      Optional.ofNullable(System.getProperty("axelor.view.watch.delay"))
          .map(Long::valueOf)
          .orElse(300L);

  private static final Set<WatchEvent.Kind<Path>> WATCHED_KINDS =
      Optional.ofNullable(System.getProperty("axelor.view.watch.kinds"))
          .map(
              property ->
                  Arrays.stream(property.split("\\s*,\\s*"))
                      .map(
                          name -> {
                            switch (name) {
                              case "ENTRY_CREATE":
                                return ENTRY_CREATE;
                              case "ENTRY_MODIFY":
                                return ENTRY_MODIFY;
                              case "ENTRY_DELETE":
                                return ENTRY_DELETE;
                              default:
                                throw new IllegalArgumentException(name);
                            }
                          })
                      .collect(Collectors.toSet()))
          .orElseGet(() -> ImmutableSet.of(ENTRY_CREATE));

  private ViewWatcher() {}

  public static ViewWatcher getInstance() {
    if (instance == null) {
      instance = new ViewWatcher();
      instance.start();

      moduleManager = Beans.get(ModuleManager.class);
      moduleManager.setLoadData(false);

      moduleNames = new HashSet<>();
      ModuleManager.getAll().stream()
          .forEach(
              module -> {
                final String name = module.getName();
                moduleNames.add(name);
                if (module.isApplication()) {
                  appName = name;
                }
              });
    }
    return instance;
  }

  private boolean handleEvents() {
    // wait for key to be signaled
    final WatchKey key;

    try {
      key = watcher.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }

    final Path dir = keys.get(key);

    if (dir == null) {
      return false;
    }

    for (WatchEvent<?> event : key.pollEvents()) {
      final WatchEvent.Kind<?> kind = event.kind();

      if (kind == OVERFLOW) {
        continue;
      }

      final Path file = dir.resolve((Path) event.context());

      try {
        if (Files.isReadable(file) && Files.size(file) > 0) {
          handlePath(kind, file);
        }
      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
      }
    }

    boolean valid = key.reset();

    if (!valid) {
      keys.remove(key);
      if (keys.isEmpty()) {
        return false;
      }
    }

    return true;
  }

  private void handlePath(WatchEvent.Kind<?> kind, Path path) {
    if (WATCHED_KINDS.contains(kind)) {
      handlePath(path);
    }
  }

  private void handlePath(Path path) {
    try {
      final String moduleName = findAxelorModule(path);
      if (moduleName != null) {
        addPending(moduleName, path);
      }
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
  }

  private String findAxelorModule(Path path) throws IOException {
    Path propsPath = null;
    FileSystem fs = null;
    InputStream is = null;

    try {
      final String pathStr = path.toString();
      if (pathStr.toLowerCase().endsWith(".jar")) {
        // resources in jar
        fs = FileSystems.newFileSystem(path, (ClassLoader) null);
        propsPath = fs.getPath("META-INF", "axelor-module.properties");
      } else {
        for (final String location : ImmutableList.of("WEB-INF/classes", "main")) {
          final String subPathStr = Paths.get('/' + location).toString();
          final int index = pathStr.indexOf(subPathStr);
          if (index >= 0) {
            final Path current =
                Paths.get(pathStr.substring(0, index))
                    .resolve(Paths.get(location, "META-INF", "axelor-module.properties"));
            if (Files.exists(current)) {
              propsPath = current;
              break;
            }
          }
        }
        if (propsPath == null) {
          throw new IllegalArgumentException("Unable to find module name of file " + path.toFile());
        }
      }

      final Properties props = new Properties();
      try {
        is = Files.newInputStream(propsPath);
      } catch (NoSuchFileException e) {
        // Not an Axelor module
        LOG.trace("No module file for: {}", path);
        return null;
      }
      props.load(is);
      return props.getProperty("name");

    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (Exception e) {
          // ignore
        }
      }
      if (fs != null) {
        try {
          fs.close();
        } catch (Exception e) {
          // ignore
        }
      }
    }
  }

  private void addPending(String moduleName, Path path) {
    synchronized (pendingModules) {
      if (scheduledFuture != null && !scheduledFuture.cancel(false)) {
        wait(scheduledFuture);
      }

      final String name = moduleNames.contains(moduleName) ? moduleName : appName;
      pendingModules.add(name);
      pendingPaths.add(path);

      scheduledFuture =
          scheduler.schedule(
              () -> {
                synchronized (pendingModules) {
                  try {
                    doInSession(
                        () -> {
                          moduleManager.update(pendingModules, pendingPaths);
                        });
                    MetaStore.clear();
                    I18nBundle.invalidate();
                  } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                  } finally {
                    pendingModules.clear();
                    pendingPaths.clear();
                    scheduledFuture = null;
                  }
                }
              },
              UPDATE_DELAY,
              TimeUnit.MILLISECONDS);
    }
  }

  void doInSession(Runnable task) {
    UnitOfWork unitOfWork = Beans.get(UnitOfWork.class);
    unitOfWork.begin();
    try {
      task.run();
    } finally {
      unitOfWork.end();
    }
  }

  private void wait(Future<?> future) {
    do {
      try {
        future.get(10, TimeUnit.MINUTES);
        return;
      } catch (ExecutionException e) {
        if (e.getCause() instanceof RuntimeException) {
          throw (RuntimeException) e.getCause();
        }

        throw new RuntimeException(e.getCause());
      } catch (TimeoutException e) {
        LOG.warn("Future {} is taking a long time to complete.", future);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

    } while (!Thread.currentThread().isInterrupted());
  }

  private synchronized void registerAll() throws Exception {
    final Set<Path> paths = new HashSet<>();
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    Optional.ofNullable(classLoader.getResource(""))
        .ifPresent(
            rootResource -> {
              switch (rootResource.getProtocol()) {
                case "file":
                  final Path path =
                      Paths.get(toURI(rootResource)).resolve(Paths.get("..", "lib")).normalize();
                  if (path.toFile().isDirectory()) {
                    paths.add(path);
                  }
                  break;
                case "jar":
                  final Path pathJar =
                      Paths.get(toURI(rootResource.getPath())).getParent().getParent();
                  if (Files.isDirectory(pathJar)) {
                    paths.add(pathJar);
                  }
                  break;
                default:
                  LOG.error("Unsupported resource: {}", rootResource);
              }
            });

    Reflections.findResources()
        .byName("(domains|i18n|views)/(.*?)\\.(xml|csv)$")
        .find()
        .parallelStream()
        .filter(url -> url.getPath().startsWith("/"))
        .map(url -> Paths.get(toURI(url)).resolve("..").normalize())
        .distinct()
        .forEach(paths::add);

    if (paths.isEmpty()) {
      return;
    }

    if (watcher == null) {
      watcher = FileSystems.getDefault().newWatchService();
    }

    LOG.info("Starting view watch...");

    paths.forEach(
        p -> {
          try {
            keys.put(p.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY), p);
            LOG.debug("Watching: {}", p);
          } catch (IOException e) {
            LOG.warn("Unable to watch: {}", p);
          }
        });
  }

  private static boolean isEnabled() {
    return Boolean.parseBoolean(System.getProperty("axelor.view.watch")) || isDebug();
  }

  private static boolean isDebug() {
    return java.lang.management.ManagementFactory.getRuntimeMXBean()
        .getInputArguments()
        .toString()
        .contains("-agentlib:jdwp");
  }

  public void start() {
    if (running || !isEnabled()) {
      return;
    }
    try {
      registerAll();
    } catch (Exception e) {
      LOG.error("Unable to start view watch.", e);
      return;
    }
    if (keys.isEmpty()) {
      return;
    }

    pendingModules = new HashSet<>();
    pendingPaths = new HashSet<>();
    scheduler = Executors.newSingleThreadScheduledExecutor();

    Thread runner =
        new Thread(
            () -> {
              for (; ; ) {
                if (!running || !handleEvents()) {
                  break;
                }
              }
            });

    runner.setDaemon(true);
    runner.start();

    running = true;
  }

  public void stop() {
    if (running) {
      running = false;
      LOG.info("Stopping view watch....");
      keys.keySet().forEach(WatchKey::cancel);
      keys.clear();
      shutdownScheduler();
    }
  }

  private void shutdownScheduler() {
    scheduler.shutdown();

    try {
      if (!scheduler.awaitTermination(1, TimeUnit.MINUTES)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static URI toURI(URL url) {
    try {
      return url.toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static URI toURI(String path) {
    try {
      return new URI(path);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
