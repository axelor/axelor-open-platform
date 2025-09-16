/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tomcat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class TomcatRunner {

  private static final String OPTION_HELP = "help";
  private static final String OPTION_PORT = "port";
  private static final String OPTION_BASE = "base-dir";
  private static final String OPTION_CONTEXT = "context-path";
  private static final String OPTION_CLASSES = "extra-classes";
  private static final String OPTION_LIBS = "extra-libs";
  private static final String OPTION_CONFIG = "config";

  private static Option addOption(Options options, String name, String argName, String desc) {
    final Option option = Option.builder().longOpt(name).desc(desc).build();
    if (argName != null) {
      option.setArgName(argName);
      option.setArgs(1);
    }
    options.addOption(option);
    return option;
  }

  private static void usage(Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.setOptionComparator(null);
    formatter.printHelp("tomcat-runner [options] <WEBAPP>", options);
  }

  private static List<String> getList(Properties props, String key) {
    String value = props.getProperty(key, "");
    if (value.trim().isEmpty()) {
      return Collections.emptyList();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  public static void main(String[] args) {
    final Options options = new Options();
    final CommandLineParser parser = new DefaultParser();
    final CommandLine cli;

    addOption(options, OPTION_HELP, null, "Show this help");
    addOption(options, OPTION_PORT, "NUMBER", "The tomcat server port.");
    addOption(options, OPTION_CONTEXT, "PATH", "The context path to use for the app.");
    addOption(options, OPTION_BASE, "DIR", "The tomcat server base directory.");
    addOption(options, OPTION_CLASSES, "DIR,...", "The list of extra classes dirs.");
    addOption(options, OPTION_LIBS, "JAR,...", "The list of extra jar libs.");
    addOption(options, OPTION_CONFIG, "FILE", "The config file.");

    try {
      cli = parser.parse(options, args);
    } catch (Exception e) {
      usage(options);
      return;
    }

    if (cli.hasOption(OPTION_HELP)) {
      usage(options);
      return;
    }

    final Properties props = new Properties();

    if (cli.hasOption(OPTION_CONFIG)) {
      File config = new File(cli.getOptionValue(OPTION_CONFIG));
      try (FileInputStream is = new FileInputStream(config)) {
        props.load(is);
      } catch (IOException e) {
        System.err.println("invalid config file: " + config);
      }
    }

    final List<Path> webapps = new ArrayList<>();

    cli.getArgList().stream().map(Path::of).forEach(webapps::add);
    getList(props, "webapps").stream().map(Path::of).forEach(webapps::add);

    if (webapps.isEmpty()) {
      usage(options);
      return;
    }

    final TomcatOptions settings = new TomcatOptions(webapps);

    settings.setContextPath(
        cli.getOptionValue(OPTION_CONTEXT, props.getProperty("contextPath", "/")));

    try {
      settings.setPort(
          Integer.parseInt(cli.getOptionValue(OPTION_PORT, props.getProperty("port", "8080"))));
    } catch (NumberFormatException e) {
      System.err.println("Invalid port.");
      return;
    }

    Path baseDir = null;
    if (cli.hasOption(OPTION_BASE)) {
      baseDir = Path.of(cli.getOptionValue(OPTION_BASE));
    } else if (props.containsKey("baseDir")) {
      baseDir = Path.of(props.getProperty("baseDir"));
    }
    if (baseDir != null) {
      if (Files.exists(baseDir) && Files.isRegularFile(baseDir)) {
        System.err.println("invalid base directory.");
        return;
      }
      settings.setBaseDir(baseDir);
    }

    if (cli.hasOption(OPTION_CLASSES)) {
      Arrays.stream(cli.getOptionValues(OPTION_CLASSES))
          .flatMap(value -> Arrays.stream(value.split(",")))
          .map(String::trim)
          .map((Path::of))
          .forEach(settings::addClasses);
    }

    getList(props, "extraClasses").stream().map((Path::of)).forEach(settings::addClasses);

    if (cli.hasOption(OPTION_LIBS)) {
      Arrays.stream(cli.getOptionValues(OPTION_LIBS))
          .flatMap(value -> Arrays.stream(value.split(",")))
          .map(String::trim)
          .map((Path::of))
          .forEach(settings::addLib);
    }

    getList(props, "extraLibs").stream().map((Path::of)).forEach(settings::addLib);

    final TomcatServer server = new TomcatServer(settings);
    server.start();
  }
}
