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
package com.axelor.app.internal;

import com.axelor.app.AppModule;
import com.axelor.app.AppSettings;
import com.axelor.auth.AuthModule;
import com.axelor.db.JpaModule;
import com.axelor.db.converters.EncryptedFieldService;
import com.axelor.db.internal.DBHelper;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.rpc.ObjectMapperProvider;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.List;

public class AppCli {

  private static final String PROGRAM_NAME = "axelor";
  private static final String PERSISTENCE_UNIT = "persistenceUnit";

  static class MyOptions {

    @Parameter(
      names = {"-h", "--help"},
      description = "show this help message",
      help = true
    )
    public Boolean showHelp;

    @Parameter(
      names = {"-i", "--init"},
      description = "initialize the database"
    )
    public Boolean init;

    @Parameter(
      names = {"-u", "--update"},
      description = "update the installed modules"
    )
    public Boolean update;

    @Parameter(
      names = {"-M", "--migrate"},
      description = "run the db migration scripts"
    )
    public Boolean migrate;

    @Parameter(
      names = {"-E", "--encrypt"},
      description = "update encrypted values"
    )
    public Boolean encrypt;

    @Parameter(
      names = {"--verbose"},
      description = "verbose ouput"
    )
    public Boolean verbose;

    @Parameter(
      names = {"-m", "--modules"},
      description = "list of modules to update",
      variableArity = true
    )
    public List<String> modules;
  }

  static class MyModule extends AbstractModule {

    private String jpaUnit;

    public MyModule(String jpaUnit) {
      this.jpaUnit = jpaUnit;
    }

    @Override
    protected void configure() {

      bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class);

      install(new JpaModule(jpaUnit));
      install(new AuthModule());
      install(new AppModule());
    }
  }

  private static void println(String msg) {
    JCommander.getConsole().println(msg);
  }

  public static int process(String[] args) {

    MyOptions opts = new MyOptions();
    JCommander cmd = new JCommander(opts);

    cmd.setProgramName(PROGRAM_NAME);

    try {
      cmd.parse(args);
    } catch (Exception e) {
      println(e.getMessage());
      cmd.usage();
      return -1;
    }

    if ((opts.showHelp == Boolean.TRUE)
        || (opts.init == Boolean.FALSE
            && opts.update == Boolean.FALSE
            && opts.migrate == Boolean.FALSE)) {
      cmd.usage();
      return 0;
    }

    if (opts.migrate == Boolean.TRUE) {
      try {
        println("Start db migration...");
        DBHelper.migrate();
        println("db migration complete.");
        return 0;
      } catch (Exception e) {
        println("db migration failed.");
        println(e.getMessage());
        if (opts.verbose == Boolean.TRUE) {
          e.printStackTrace();
        }
        return -1;
      }
    }

    Injector injector = Guice.createInjector(new MyModule(PERSISTENCE_UNIT));

    if (opts.encrypt) {
      System.setProperty("database.encrypt.migrate", "true");
      EncryptedFieldService service = injector.getInstance(EncryptedFieldService.class);
      try {
        service.migrate();
        println("Field encryption complete.");
        println("Remove 'encryption.password.old' from 'application.properties'");
        return 0;
      } catch (Exception e) {
        println("field value encryption failed.");
        println(e.getMessage());
        if (opts.verbose == Boolean.TRUE) {
          e.printStackTrace();
        }
        return -1;
      }
    }

    ModuleManager manager = injector.getInstance(ModuleManager.class);

    boolean demo = AppSettings.get().getBoolean("data.import.demo-data", true);

    if (opts.init == Boolean.TRUE) {
      manager.initialize(opts.update == Boolean.TRUE, demo);
      return 0;
    }

    String[] names = {};
    if (opts.modules != null) {
      names = opts.modules.toArray(new String[] {});
    }
    manager.update(demo, names);
    return 0;
  }

  public static void main(String[] args) {
    int status = 0;
    try {
      AppLogger.install();
      status = process(args);
    } catch (Exception e) {
      status = -1;
    } finally {
      AppLogger.uninstall();
    }
    System.exit(status);
  }
}
