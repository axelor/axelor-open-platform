/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.tasks;

import com.axelor.common.FileUtils;
import com.axelor.common.StringUtils;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.support.ScriptsSupport;
import java.io.File;
import javax.inject.Inject;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.options.Option;
import org.gradle.process.ExecOperations;
import org.gradle.process.JavaExecSpec;

public class DatabaseTask extends AbstractRunTask {

  public static final String TASK_NAME = "database";

  public static final String TASK_DESCRIPTION = "Manage application database.";

  public static final String TASK_GROUP = AxelorPlugin.AXELOR_APP_GROUP;

  private static final String MAIN_CLASS = "com.axelor.db.internal.DBManager";

  private boolean init;

  private boolean update;

  private boolean migrate;

  private boolean encrypt;

  private String modules;

  @Inject
  public DatabaseTask(ExecOperations execOperations) {
    super(execOperations);
  }

  @Option(option = "init", description = "initialize the database")
  public void setInit(boolean init) {
    this.init = init;
  }

  @Option(option = "update", description = "update the database")
  public void setUpdate(boolean update) {
    this.update = update;
  }

  @Option(option = "migrate", description = "run database migration scripts")
  public void setMigrate(boolean migrate) {
    this.migrate = migrate;
  }

  @Option(option = "encrypt", description = "update encrypted field values")
  public void setEncrypt(boolean encrypt) {
    this.encrypt = encrypt;
  }

  @Option(option = "modules", description = "comma-separated list of modules to update")
  public void setModules(String modules) {
    this.modules = modules;
  }

  @Override
  protected String getMainClass() {
    return MAIN_CLASS;
  }

  @Override
  protected FileCollection getClasspath() {
    return super.getClasspath()
        .plus(getProject().getConfigurations().findByName(ScriptsSupport.DATABASE_CONFIGURATION));
  }

  @Override
  protected File getManifestJar() {
    return FileUtils.getFile(
        getProject().getLayout().getBuildDirectory().get().getAsFile(),
        "database",
        "classpath.jar");
  }

  @Override
  protected void configure(JavaExecSpec task) {
    String action = "init";
    if (init) action = "init";
    if (update) action = "update";
    if (migrate) action = "migrate";
    if (encrypt) action = "encrypt";

    task.args(action);

    if (StringUtils.notBlank(modules)) {
      task.args("--modules", modules);
    }
  }
}
