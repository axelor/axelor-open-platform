/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.app.cli;

import com.axelor.app.AvailableAppSettings;
import com.axelor.app.internal.AppLogger;
import com.axelor.common.VersionUtils;
import com.axelor.meta.MetaScanner;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

public class CliRunner {

  static class Builder {

    private String name;
    private String version;
    private final List<CliCommand> commands = new ArrayList<>();

    public Builder addCommand(CliCommand command) {
      this.commands.add(command);
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public CliRunner build() {
      CommandSpec spec = CommandSpec.create();
      spec.name(name);
      spec.version(version);

      OptionSpec opt =
          OptionSpec.builder("-c", "--config")
              .paramLabel("FILE")
              .description("Path to axelor config file.")
              .type(Path.class)
              .build();

      spec.addOption(opt);
      spec.mixinStandardHelpOptions(true);

      return new CliRunner(spec, commands);
    }
  }

  private final CommandSpec spec;

  private final List<CliCommand> commands;

  public CliRunner(CommandSpec spec, List<CliCommand> commands) {
    this.spec = spec;
    this.commands = commands;
  }

  static Builder builder() {
    return new Builder();
  }

  int execute(String... args) {
    CommandLine cli = new CommandLine(spec);
    Optional.ofNullable(commands).ifPresent(commands -> commands.forEach(cli::addSubcommand));
    return cli.execute(args);
  }

  private static CliRunner build() {
    Builder builder = CliRunner.builder();
    Properties props = new Properties();

    builder.name("axelor");
    builder.version(VersionUtils.getVersion().version);

    // find all sub commands using MetaScanner (module-only scanning)
    Set<Class<? extends CliCommand>> commandClasses =
        MetaScanner.findSubTypesOf(CliCommand.class).find();
    for (Class<? extends CliCommand> commandClass : commandClasses) {
      try {
        CliCommand command = commandClass.getDeclaredConstructor().newInstance();
        builder.addCommand(command);
      } catch (Exception e) {
        // Skip commands that cannot be instantiated
      }
    }

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    URL config =
        Stream.of("axelor-config.properties", "axelor-config.yaml", "axelor-config.yml")
            .map(loader::getResource)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

    if (config == null) {
      return builder.build();
    }

    try (InputStream stream = config.openStream()) {
      props.load(stream);
    } catch (IOException e) {
      return builder.build();
    }

    String version = props.getProperty(AvailableAppSettings.APPLICATION_VERSION);
    builder.version(version);

    return builder.build();
  }

  private static Path findConfig(String... args) {

    CommandSpec spec = CommandSpec.create();
    OptionSpec opt = OptionSpec.builder("-c", "--config").type(Path.class).build();

    spec.addOption(opt);

    CommandLine cli = new CommandLine(spec);
    cli.setUnmatchedArgumentsAllowed(true);
    cli.setUnmatchedOptionsArePositionalParams(true);

    ParseResult result = cli.parseArgs(args);

    if (result.hasMatchedOption(opt)) {
      return result.matchedOptionValue(opt.shortestName(), null);
    }

    return null;
  }

  private static void ensureConfig(String... args) {
    Path config = findConfig(args);
    if (config == null || Files.notExists(config)) {
      return;
    }
    System.setProperty("axelor.config", config.toString());
  }

  public static void main(String[] args) {
    ensureConfig(args);
    try {
      AppLogger.install();
      System.exit(CliRunner.build().execute(args));
    } finally {
      AppLogger.uninstall();
    }
  }
}
