/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tomcat;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "tomcat-runner", mixinStandardHelpOptions = true)
public class TomcatRunner implements Runnable {

  @Parameters private List<Path> webapps = new ArrayList<>();

  @Option(names = "--base-dir", description = "The tomcat base directory.")
  private Path baseDir;

  @Option(names = "--options-from", description = "The options config file.")
  private Path config;

  @Option(names = "--context-path", description = "The webapp context path.")
  private String contextPath;

  @Option(names = "--proxy-url", description = "Set proxy URL if running behind a reverse proxy")
  private URI proxyUrl;

  @Option(names = "--max-threads", description = "Set the maximum number of worker threads.")
  private int maxThreads;

  @Option(names = "--cache-max-size", description = "Set the maximum cache size for resources.")
  private int cacheMaxSize;

  @Option(
      names = "--extra-classes",
      split = ",",
      description = "Specify additional classes directories.")
  private List<Path> extraClasses;

  @Option(names = "--extra-libs", split = ",", description = "Specify additional libraries.")
  private List<Path> extraLibs;

  @Option(names = "--port", description = "The tomcat port number.", defaultValue = "8080")
  private Integer port;

  private static List<String> getList(Properties props, String key) {
    String value = props.getProperty(key, "");
    if (value.trim().length() == 0) {
      return Collections.emptyList();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(s -> s.length() > 0)
        .collect(Collectors.toList());
  }

  @Override
  public void run() {
    final Properties props = new Properties();

    if (config != null) {
      try (FileInputStream is = new FileInputStream(config.toFile())) {
        props.load(is);
      } catch (IOException e) {
        System.err.println("invalid config file: " + config);
      }
    }

    final List<Path> webapps = new ArrayList<>(this.webapps);

    getList(props, "webapps").stream().map(Paths::get).forEach(webapps::add);

    if (webapps.isEmpty()) {
      System.err.println("No webapp specified.");
      return;
    }

    final TomcatOptions settings = new TomcatOptions(webapps);

    settings.setProxyUrl(proxyUrl);
    settings.setMaxThreads(maxThreads);
    if (cacheMaxSize > 0) {
      settings.setCacheMaxSize(cacheMaxSize);
    }

    settings.setContextPath(
        Optional.ofNullable(contextPath).orElse(props.getProperty("contextPath", "")));

    try {
      settings.setPort(
          Optional.ofNullable(port)
              .orElseGet(() -> Integer.parseInt(props.getProperty("port", "8080"))));
    } catch (NumberFormatException e) {
      System.err.println("Invalid port.");
      return;
    }

    Path baseDir = this.baseDir;
    if (baseDir == null && props.containsKey("baseDir")) {
      baseDir = Paths.get(props.getProperty("baseDir"));
    }
    if (baseDir != null) {
      if (Files.exists(baseDir) && Files.isRegularFile(baseDir)) {
        System.err.println("invalid base directory.");
        return;
      }
      settings.setBaseDir(baseDir);
    }

    if (extraClasses != null) extraClasses.forEach(settings::addClasses);
    if (extraLibs != null) extraLibs.forEach(settings::addLib);

    getList(props, "extraClasses").stream().map(Paths::get).forEach(settings::addClasses);
    getList(props, "extraLibs").stream().map(Paths::get).forEach(settings::addLib);

    new TomcatServer(settings).start();
  }

  public static void main(String[] args) {
    TomcatRunner runner = new TomcatRunner();
    CommandLine cli = new CommandLine(runner);
    System.exit(cli.execute(args));
  }
}
