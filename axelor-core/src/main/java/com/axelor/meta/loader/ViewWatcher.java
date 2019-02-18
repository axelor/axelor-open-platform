/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import com.axelor.app.AppSettings;
import com.axelor.common.reflections.Reflections;
import com.axelor.inject.Beans;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ViewWatcher {

  private static final Logger log = LoggerFactory.getLogger(ViewWatcher.class);

  private static ViewWatcher instance;
  private static ModuleManager moduleManager;

  private WatchService watcher;
  private final Map<WatchKey, Path> keys = new HashMap<>();
  private final List<ViewChangeEvent> pending = new ArrayList<>();

  private static final long UPDATE_DELAY = 200;
  private static final Pattern moduleNamePattern = Pattern.compile("\\w*(-[a-z]\\w*)*");
  private Set<String> pendingModules;
  private Set<Path> pendingPaths;
  private ScheduledExecutorService scheduler;
  private ScheduledFuture<?> scheduledFuture;

  private Thread runner;
  private boolean running;

  private BiConsumer<WatchEvent.Kind<?>, Path> watchEventHandler;

  private ViewWatcher() {}

  public static ViewWatcher getInstance() {
    if (instance == null) {
      instance = new ViewWatcher();
      instance.start();

      moduleManager = Beans.get(ModuleManager.class);
      moduleManager.setLoadData(false);
    }
    return instance;
  }

  static synchronized void process() {
    final ViewWatcher watcher = getInstance();
    if (watcher.pending.isEmpty()) {
      return;
    }
    final ViewLoader loader = Beans.get(ViewLoader.class);
    try {
      watcher.pending.forEach(
          event -> {
            if (event.isDelete()) {
              // complete re-import is required
              log.warn("File deleted: {}", event.getFile());
            } else {
              log.info("Updating views from: {}", event.getFile());
              try {
                loader.updateFrom(event.getFile(), event.getModule());
              } catch (Exception e) {
                log.error("Unable to update views from: {}", event.getFile(), e);
              }
            }
          });
    } finally {
      watcher.pending.clear();
    }
  }

  private synchronized void addPending(ViewChangeEvent event) {
    if (pending.contains(event)) {
      pending.remove(event);
    }
    pending.add(0, event);
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
          watchEventHandler.accept(kind, file);
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
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

  private void handleAgent(WatchEvent.Kind<?> kind, Path path) {
    final Path modulePath = path.resolve(Paths.get("..", "..", "..", "..", "..")).normalize();
    addPending(new ViewChangeEvent(kind, path, modulePath.toFile().getName()));
  }

  private void handleJar(WatchEvent.Kind<?> kind, Path path) {
    if (kind != ENTRY_CREATE && kind != ENTRY_MODIFY) {
      return;
    }

    final String fileName = Paths.get(path.toUri().getPath()).getFileName().toString();
    final Matcher moduleNameMatcher = moduleNamePattern.matcher(fileName);
    final String moduleName;

    if (!moduleNameMatcher.find()) {
      log.error("Cannot identify module name: {}", path);
      return;
    }

    moduleName = moduleNameMatcher.group();
    addPending(moduleName, path);
  }

  private void handleBin(WatchEvent.Kind<?> kind, Path path) {
    if (kind != ENTRY_CREATE && kind != ENTRY_MODIFY) {
      return;
    }

    final Path modulePath = path.resolve(Paths.get("..", "..", "..", "..")).normalize();
    final String moduleName = modulePath.toFile().getName();
    addPending(moduleName, path);
  }

  private void addPending(String moduleName, Path path) {
    synchronized (pendingModules) {
      if (scheduledFuture != null && !scheduledFuture.cancel(false)) {
        wait(scheduledFuture);
      }

      pendingModules.add(moduleName);
      pendingPaths.add(path);

      scheduledFuture =
          scheduler.schedule(
              () -> {
                synchronized (pendingModules) {
                  try {
                    moduleManager.update(pendingModules, pendingPaths);
                  } catch (Exception e) {
                    log.error(e.getMessage(), e);
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
        log.warn("Furure {} is taking a long time to complete.", future);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

    } while (!Thread.currentThread().isInterrupted());
  }

  private synchronized void registerAll() throws Exception {
    final Pattern pattern = Pattern.compile(".*propertiesFilePath=([^,]+).*");
    final Set<Path> paths =
        ManagementFactory.getRuntimeMXBean()
            .getInputArguments()
            .stream()
            .filter(s -> s.startsWith("-javaagent"))
            .map(s -> pattern.matcher(s))
            .filter(m -> m.matches())
            .map(m -> m.group(1).trim())
            .map(Paths::get)
            .flatMap(
                file -> {
                  final Properties props = new Properties();
                  try (InputStream is = new FileInputStream(file.toFile())) {
                    props.load(is);
                  } catch (IOException e) {
                    log.error("unable to read: {}", file, e);
                    throw new UncheckedIOException(e);
                  }
                  final String resources = props.getProperty("watchResources", "");
                  return Stream.of(resources.split(","));
                })
            .map(String::trim)
            .map(Paths::get)
            .map(p -> p.resolve("views"))
            .filter(p -> p.toFile().isDirectory())
            .collect(Collectors.toSet());

    if (!paths.isEmpty()) {
      watchEventHandler = this::handleAgent;
    } else {
      final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      final Optional<URL> rootResourceOpt = Optional.ofNullable(classLoader.getResource(""));
      rootResourceOpt.ifPresent(
          rootResource -> {
            final Path libPath = Paths.get(rootResource.getPath(), "..", "lib").normalize();
            if (libPath.toFile().isDirectory()) {
              paths.add(libPath);
            }
          });

      if (!paths.isEmpty()) {
        watchEventHandler = this::handleJar;
      } else {
        Reflections.findResources()
            .byName("(domains|i18n|views)/(.*?)\\.(xml|csv)$")
            .find()
            .parallelStream()
            .map(URL::getPath)
            .filter(path -> path.startsWith("/"))
            .map(path -> Paths.get(path, "..").normalize())
            .distinct()
            .forEach(paths::add);

        if (!paths.isEmpty()) {
          watchEventHandler = this::handleBin;
        }
      }
    }

    if (paths.isEmpty()) {
      return;
    }

    if (watcher == null) {
      watcher = FileSystems.getDefault().newWatchService();
    }

    log.info("Starting view watch...");

    paths.forEach(
        p -> {
          try {
            keys.put(p.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY), p);
            log.info("Watching: {}", p);
          } catch (IOException e) {
            log.warn("Unable to watch: {}", p);
          }
        });
  }

  public void start() {
    if (running || AppSettings.get().isProduction()) {
      return;
    }
    try {
      registerAll();
    } catch (Exception e) {
      log.error("Unable to start view watch.", e);
      return;
    }
    if (keys.isEmpty()) {
      return;
    }

    pendingModules = new HashSet<>();
    pendingPaths = new HashSet<>();
    scheduler = Executors.newSingleThreadScheduledExecutor();

    runner =
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
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
  }

  public void stop() {
    if (running) {
      running = false;
      log.info("Stopping view watch....");
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

  static final class ViewChangeEvent {

    private final WatchEvent.Kind<?> kind;
    private final Path file;
    private final String module;

    public ViewChangeEvent(WatchEvent.Kind<?> kind, Path file, String module) {
      this.kind = kind;
      this.file = file;
      this.module = module;
    }

    public boolean isCreate() {
      return kind == ENTRY_CREATE;
    }

    public boolean isDelete() {
      return kind == ENTRY_DELETE;
    }

    public boolean isModify() {
      return kind == ENTRY_MODIFY;
    }

    public WatchEvent.Kind<?> getKind() {
      return kind;
    }

    public Path getFile() {
      return file;
    }

    public String getModule() {
      return module;
    }

    @Override
    public int hashCode() {
      return Objects.hash(0, file);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof ViewChangeEvent)) return false;
      final ViewChangeEvent other = (ViewChangeEvent) obj;
      return this.file.equals(other.file);
    }

    @Override
    public String toString() {
      return "[" + kind + ", " + file + "]";
    }
  }
}
