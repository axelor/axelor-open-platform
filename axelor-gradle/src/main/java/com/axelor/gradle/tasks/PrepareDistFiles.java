/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.axelor.common.StringUtils;
import com.axelor.gradle.AxelorExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault
public abstract class PrepareDistFiles extends DefaultTask {

  @OutputDirectory
  public abstract DirectoryProperty getOutputDir();

  @InputFile
  @Optional
  public abstract RegularFileProperty getLicenseFile();

  @TaskAction
  public void generate() {
    Directory target = getOutputDir().get();
    target.getAsFile().mkdirs();

    copyReadme(target);
    copyLicense(target);
  }

  private void copyLicense(Directory target) {
    File file = getLicense();
    if (file == null || !file.exists()) {
      return;
    }

    try {
      Files.copy(
          file.toPath(),
          new File(target.getAsFile(), file.getName()).toPath(),
          StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      getLogger().error("Error copying {} for distribution", file.getName(), e);
    }
  }

  private File getLicense() {
    if (getLicenseFile().isPresent()) {
      return getLicenseFile().get().getAsFile();
    }

    String[] candidates = {"LICENSE", "LICENSE.txt", "LICENSE.md"};
    for (String name : candidates) {
      File f = getProject().file(name);
      if (f.exists()) {
        return f;
      }
    }
    return null;
  }

  private void copyReadme(Directory target) {
    try (InputStream in =
        this.getClass().getClassLoader().getResourceAsStream("com/axelor/app/README.md")) {
      File targetFile = new File(target.getAsFile(), "README.md");
      Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

      replaceInReadme(targetFile.toPath());
    } catch (Exception e) {
      getLogger().error("Error copying README.md for distribution", e);
    }
  }

  private void replaceInReadme(Path file) {
    try {
      String content = Files.readString(file, StandardCharsets.UTF_8);
      String newContent = content.replaceAll("\\{\\{app-name}}", getAppName());
      Files.writeString(file, newContent, StandardCharsets.UTF_8);
    } catch (Exception e) {
      getLogger().error("Error while updating README file for distribution", e);
    }
  }

  private String getAppName() {
    String appName = getProject().getName();
    AxelorExtension extension = getProject().getExtensions().findByType(AxelorExtension.class);
    if (extension != null && StringUtils.notBlank(extension.getTitle())) {
      appName = extension.getTitle();
    }
    return appName;
  }
}
