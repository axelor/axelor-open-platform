/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.app.cli.commands;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.app.cli.AbstractCliCommand;
import com.axelor.db.converters.EncryptedFieldService;
import com.axelor.db.internal.DBHelper;
import com.axelor.inject.Beans;
import com.axelor.meta.loader.ModuleManager;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(
    name = "database",
    description = "Perform database maintenance operations.",
    subcommands = {
      DatabaseCommand.InitCommand.class,
      DatabaseCommand.UpdateCommand.class,
      DatabaseCommand.EncryptCommand.class,
      DatabaseCommand.MigrationCommand.class,
    })
public class DatabaseCommand extends AbstractCliCommand {

  @Spec private CommandSpec spec;

  @Override
  public void run() {
    throw new ParameterException(spec.commandLine(), "Missing required subcommand");
  }

  void run(Runnable task) {
    withSession(task);
  }

  @Command(name = "migrate", description = "Run the migration scripts.")
  static class MigrationCommand extends AbstractCliCommand {

    @ParentCommand DatabaseCommand parent;

    @Override
    public void run() {
      parent.run(
          () -> {
            log.info("Start db migration...");
            DBHelper.migrate();
            log.info("db migration complete.");
          });
    }
  }

  @Command(name = "encrypt", description = "Update the encrypted field values.")
  static class EncryptCommand extends AbstractCliCommand {

    @ParentCommand DatabaseCommand parent;

    @Override
    public void run() {
      parent.run(
          () -> {
            EncryptedFieldService service = Beans.get(EncryptedFieldService.class);
            log.info("Start field value encryption...");
            System.setProperty("axelor.task.database", "encrypt");
            service.migrate();
            log.info("Field value encryption complete.");
            log.info("Remove 'encryption.old-password' from 'axelor-config.properties'");
          });
    }
  }

  @Command(name = "init", description = "Initialize the database.")
  static class InitCommand extends AbstractCliCommand {

    @ParentCommand DatabaseCommand parent;

    @Override
    public void run() {
      parent.run(
          () -> {
            boolean demo =
                AppSettings.get().getBoolean(AvailableAppSettings.DATA_IMPORT_DEMO_DATA, true);
            ModuleManager manager = Beans.get(ModuleManager.class);
            manager.initialize(false, demo);
          });
    }
  }

  @Command(name = "update", description = "Update the database.")
  static class UpdateCommand extends AbstractCliCommand {

    @Option(
        names = {"-m", "--module"},
        description = "Specify the module to update, can be repeated.")
    private List<String> modules = new ArrayList<>();

    @ParentCommand private DatabaseCommand parent;

    @Override
    public void run() {
      parent.run(
          () -> {
            boolean demo =
                AppSettings.get().getBoolean(AvailableAppSettings.DATA_IMPORT_DEMO_DATA, true);
            ModuleManager manager = Beans.get(ModuleManager.class);
            manager.update(demo, modules.toArray(String[]::new));
          });
    }
  }
}
