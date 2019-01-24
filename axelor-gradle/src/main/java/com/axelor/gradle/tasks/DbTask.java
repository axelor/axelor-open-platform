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
package com.axelor.gradle.tasks;

import static java.util.regex.Matcher.quoteReplacement;

import com.axelor.common.StringUtils;
import com.axelor.gradle.AxelorPlugin;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.GradleException;
import org.gradle.api.internal.tasks.options.Option;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskAction;

public class DbTask extends JavaExec {

  public static final String TASK_NAME = "database";
  public static final String TASK_DESCRIPTION = "Manage application database.";
  public static final String TASK_GROUP = AxelorPlugin.AXELOR_APP_GROUP;

  private static final String MAIN_CLASS_NAME = "com.axelor.app.internal.AppCli";

  private String config;

  private String modules;

  private boolean update;

  private boolean migrate;

  private boolean encrypt;

  private boolean verbose;

  @Option(option = "config", description = "specify appliction config file path")
  public void setConfig(String config) {
    this.config = config;
  }

  @Option(option = "modules", description = "comma separate list of modules to update")
  public void setModules(String modules) {
    this.modules = modules;
  }

  @Option(option = "update", description = "update the installed modules")
  public void setUpdate(boolean update) {
    this.update = update;
  }

  @Option(option = "migrate", description = "run migration scripts")
  public void setMigrate(boolean migrate) {
    this.migrate = migrate;
  }

  @Option(option = "encrypt", description = "update encrypted values")
  public void setEncrypt(boolean encrypt) {
    this.encrypt = encrypt;
  }

  @Option(option = "verbose", description = "verbose ouput")
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  @Override
  public String getMain() {
    return MAIN_CLASS_NAME;
  }

  @TaskAction
  @Override
  public void exec() {
    final List<String> args = new ArrayList<>();
    final List<String> jvmArgs = new ArrayList<>();

    if (migrate) {
      args.add("-M");
    } else if (encrypt) {
      args.add("-E");
    } else if (update) {
      args.add("-u");
    } else {
      args.add("-i");
    }

    if (verbose) {
      args.add("--verbose");
    }

    if (StringUtils.notBlank(modules)) {
      args.add("-m");
      args.add(modules);
    }

    final String configPath =
        StringUtils.notBlank(config)
            ? config
            : System.getProperty("axelor.config", "src/main/resources/application.properties");

    final Path configFile = Paths.get(configPath.replaceAll("/", quoteReplacement(File.separator)));
    if (Files.notExists(configFile)) {
      throw new GradleException("Unable to find application config.");
    }

    jvmArgs.add("-Daxelor.config=" + configFile.toFile().getAbsolutePath());

    setArgs(args);
    setJvmArgs(jvmArgs);

    super.exec();
  }
}
