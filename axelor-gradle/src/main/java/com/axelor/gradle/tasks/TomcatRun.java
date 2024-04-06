/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.tasks;

import com.axelor.common.FileUtils;
import com.axelor.gradle.support.TomcatSupport;
import java.io.File;
import java.util.List;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.options.Option;

public class TomcatRun extends AbstractRunTask {

  public static final String MAIN_CLASS = "com.axelor.tomcat.TomcatRunner";

  @Option(option = "port", description = "Specify the tomcat server port (default 8080).")
  public void setPort(String port) {
    this.args("--port", port);
  }

  @Override
  public void setDebug(boolean enabled) {
    super.setDebug(enabled);
    if (enabled) {
      this.jvmArgs("-Daxelor.view.watch=true");
    }
  }

  @Override
  protected String getMainClassName() {
    return MAIN_CLASS;
  }

  @Override
  protected FileCollection getManifestClasspath() {
    return super.getManifestClasspath()
        .plus(getProject().getConfigurations().findByName(TomcatSupport.TOMCAT_CONFIGURATION));
  }

  @Override
  protected File getManifestJar() {
    return FileUtils.getFile(getBuildDir(), "tomcat", "classpath.jar");
  }

  @Override
  public List<String> getArgs() {
    final File baseDir = FileUtils.getFile(getBuildDir(), "tomcat");
    final File confFile = FileUtils.getFile(baseDir, TomcatSupport.TOMCAT_RUNNER_CONFIG);
    if (!super.getArgs().contains("--options-from")) {
      super.args("--options-from", confFile);
    }
    return super.getArgs();
  }
}
