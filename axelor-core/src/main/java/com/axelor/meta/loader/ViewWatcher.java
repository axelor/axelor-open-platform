/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.inject.Beans;

public final class ViewWatcher {
	
	private static final Logger log = LoggerFactory.getLogger(ViewWatcher.class);

	private static ViewWatcher instance;

	private WatchService watcher;
	private final Map<WatchKey, Path> keys = new HashMap<>();
	private final List<ViewChangeEvent> pending = new ArrayList<>();

	private Thread runner;
	private boolean running;

	private ViewWatcher() {
	}

	public static ViewWatcher getInstance() {
		if (instance == null) {
			instance = new ViewWatcher();
			instance.start();
		}
		return instance;
	}

	synchronized static void process() {
		final ViewWatcher watcher = getInstance();
		if (watcher.pending.isEmpty()) {
			return;
		}
		final ViewLoader loader = Beans.get(ViewLoader.class);
		try {
			watcher.pending.forEach(event -> {
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
			final Path module = dir.resolve(Paths.get("..", "..", "..", "..")).normalize();
			addPending(new ViewChangeEvent(kind, file, module.toFile().getName()));
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

	private synchronized void registerAll() throws Exception {
		final Pattern pattern = Pattern.compile(".*propertiesFilePath=([^,]+).*");
		final Set<Path> paths = ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
			.filter(s -> s.startsWith("-javaagent"))
			.map(s -> pattern.matcher(s))
			.filter(m -> m.matches())
			.map(m -> m.group(1).trim())
			.map(Paths::get)
			.flatMap(file -> {
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
			.filter(Files::exists)
			.filter(Files::isDirectory)
			.collect(Collectors.toSet());

		if (paths.isEmpty()) {
			return;
		}

		if (watcher == null) {
			watcher = FileSystems.getDefault().newWatchService();
		}

		log.info("Starting view watch...");

		paths.forEach(p -> {
			try {
				keys.put(p.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY), p);
				log.info("Watching: {}", p);
			} catch (IOException e) {
				log.warn("Unable to watch: {}", p);
			}
		});
	}

	public void start() {
		if (running) {
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

		runner = new Thread(() -> {
			for (;;) {
				if (!running || !handleEvents()) {
					break;
				}
			}
		});

		Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

		running = true;
		runner.setDaemon(true);
		runner.start();
	}

	public void stop() {
		if (running) {
			running = false;
			log.info("Stopping view watch....");
			keys.keySet().forEach(WatchKey::cancel);
			keys.clear();
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
			if (this == obj)
				return true;
			if (!(obj instanceof ViewChangeEvent))
				return false;
			final ViewChangeEvent other = (ViewChangeEvent) obj;
			return this.file.equals(other.file);
		}

		@Override
		public String toString() {
			return "[" + kind + ", " + file + "]";
		}
	}
}
