/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.support;

import com.axelor.common.ResourceUtils;
import com.axelor.common.StringUtils;
import com.axelor.gradle.AxelorUtils;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.application.CreateStartScripts;
import org.gradle.api.tasks.bundling.War;

public class CliSupport extends AbstractSupport {

  public static final String CLI_BUILD_TASK = "buildApp";

  public static final String CLI_PREPARE_TASK = "prepareApp";

  private static final String CLI_INSTALL_DIR = "install";

  private static final String APP_DIR = "app";
  private static final String LIB_DIR = "lib";
  private static final String BIN_DIR = "bin";

  @Override
  public void apply(Project project) {
    Path buildDir = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
    Path appDir = buildDir.resolve(CLI_INSTALL_DIR).resolve(project.getName());
    Path libDir = appDir.resolve(LIB_DIR);
    Path binDir = appDir.resolve(BIN_DIR);

    project
        .getTasks()
        .register(
            CLI_PREPARE_TASK,
            Sync.class,
            task -> {
              task.dependsOn(WarPlugin.WAR_TASK_NAME);
              task.setDestinationDir(appDir.toFile());

              // extract war
              War war = (War) project.getTasks().getByName(WarPlugin.WAR_TASK_NAME);
              task.from(project.zipTree(war.getArchiveFile()), x -> x.into(APP_DIR));

              // tomcat runner jars
              task.from(
                  project
                      .getConfigurations()
                      .getByName(TomcatSupport.TOMCAT_CONFIGURATION)
                      .getIncoming()
                      .artifactView(view -> view.componentFilter(x -> true))
                      .getFiles(),
                  x -> x.into(LIB_DIR));

              // copy resources
              task.doLast(
                  x -> {
                    try {
                      createReadme(appDir, project);
                      createLicense(appDir, project);
                    } catch (IOException e) {
                      throw new GradleException("Failed to generate application bundle", e);
                    }
                  });
            });

    project
        .getTasks()
        .register(
            CLI_BUILD_TASK,
            CreateStartScripts.class,
            task -> {
              task.dependsOn(CLI_PREPARE_TASK);
              task.setGroup(BasePlugin.BUILD_GROUP);
              task.setDescription("Build application package.");

              task.setApplicationName("axelor");
              task.setOutputDir(binDir.toFile());
              task.getMainClass().set("com.axelor.app.Launcher");
              task.setClasspath(project.files(project.fileTree(libDir, x -> x.include("*.jar"))));
            });
  }

  private void createReadme(Path appDir, Project project) throws IOException {
    try (var stream = ResourceUtils.getResourceStream("com/axelor/app/README.txt");
        var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      var text = CharStreams.toString(reader);
      var path = appDir.resolve("README.txt");
      var name = project.getName();

      var configPath = AxelorUtils.findAxelorConfig(project);
      if (configPath != null) {
        var config = AxelorUtils.parseAxelorConfig(project, configPath);
        var appName = (String) config.get("application.name");
        if (StringUtils.notBlank(appName)) {
          name = appName;
        }
      }

      text = text.replace("{{app-name}}", name);
      text = text.replace("{{app-header}}", "=".repeat(name.length()));

      Files.writeString(path, text, StandardCharsets.UTF_8);
    }
  }

  private void createLicense(Path appDir, Project project) throws IOException {
    var from = project.getProjectDir().toPath();
    var file =
        List.of("LICENSE", "LICENSE.txt", "LICENSE.md").stream()
            .map(x -> from.resolve(x))
            .filter(x -> Files.exists(x))
            .findFirst()
            .orElse(null);
    if (file != null) {
      var name = file.getFileName();
      var target = appDir.resolve(name);
      Files.copy(file, target);
    }
  }
}
