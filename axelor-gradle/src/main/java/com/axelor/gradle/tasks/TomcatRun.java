/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.gradle.tasks;

import com.axelor.common.FileUtils;
import com.axelor.gradle.AxelorUtils;
import com.axelor.gradle.support.TomcatSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.options.Option;
import org.gradle.process.JavaExecSpec;

public class TomcatRun extends AbstractRunTask {

  private static final String MAIN_CLASS = "com.axelor.tomcat.TomcatRunner";

  private int port = 8080;

  private boolean debug;

  @Option(option = "port", description = "Specify the tomcat server port (default 8080).")
  public void setPort(String port) {
    this.port = Integer.parseInt(port);
  }

  @Option(option = "debug-jvm", description = "Specify whether to enable debugging on port 5005.")
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  public static List<String> getArgs(Project project, int port) {
    final File baseDir = FileUtils.getFile(project.getBuildDir(), "tomcat");
    final File confFile = FileUtils.getFile(baseDir, TomcatSupport.TOMCAT_RUNNER_CONFIG);

    final List<String> args = new ArrayList<>();

    args.add("--port");
    args.add("" + port);
    args.add("--config");
    args.add(AxelorUtils.toRelativePath(project, confFile));

    return args;
  }

  public static List<String> getJvmArgs(Project project, boolean debug) {
    final List<String> jvmArgs = new ArrayList<>();
    if (debug) {
      jvmArgs.add("-Daxelor.view.watch=true");
    }
    return jvmArgs;
  }

  @Input
  protected List<String> getArgs() {
    return getArgs(getProject(), port);
  }

  @Input
  protected List<String> getJvmArgs() {
    final List<String> jvmArgs = super.getJvmArgs();
    jvmArgs.addAll(getJvmArgs(getProject(), debug));
    return jvmArgs;
  }

  @Override
  protected String getMainClass() {
    return MAIN_CLASS;
  }

  @Override
  protected FileCollection getClasspath() {
    return super.getClasspath()
        .plus(getProject().getConfigurations().findByName(TomcatSupport.TOMCAT_CONFIGURATION));
  }

  @Override
  @Internal
  protected File getManifestJar() {
    return FileUtils.getFile(getProject().getBuildDir(), "tomcat", "classpath.jar");
  }

  @Override
  protected void configure(JavaExecSpec task) {
    task.setDebug(debug);
  }
}
