/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.db.internal;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.app.internal.AppLogger;
import com.axelor.db.converters.EncryptedFieldService;
import com.axelor.inject.Beans;
import com.axelor.meta.loader.ModuleManager;
import java.util.Arrays;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBManager {

  private static Logger log = LoggerFactory.getLogger(DBManager.class);

  private static void init() {
    boolean demo = AppSettings.get().getBoolean(AvailableAppSettings.DATA_IMPORT_DEMO_DATA, true);
    final ModuleManager manager = Beans.get(ModuleManager.class);
    manager.initialize(false, demo);
  }

  private static void update() {
    boolean demo = AppSettings.get().getBoolean(AvailableAppSettings.DATA_IMPORT_DEMO_DATA, true);
    final String[] modules = System.getProperty("axelor.task.database.modules", "").split(",");
    final ModuleManager manager = Beans.get(ModuleManager.class);
    manager.update(demo, modules);
  }

  private static void encrypt() {
    EncryptedFieldService service = Beans.get(EncryptedFieldService.class);
    log.info("Start field value encryption...");
    service.migrate();
    log.info("Field value encryption complete.");
    log.info("Remove 'encryption.old-password' from 'axelor-config.properties'");
  }

  private static void migrate() {
    log.info("Start db migration...");
    DBHelper.migrate();
    log.info("db migration complete.");
  }

  private static void usage() {
    System.err.printf("Usage: %s <task> [task-options]\n", DBManager.class.getName());
    System.err.println();
    System.err.println("where task can be:");
    System.err.println("  init      - initialize the database");
    System.err.println("  update    - update the database");
    System.err.println("  encrypt   - update the encrypted field values");
    System.err.println("  migrate   - run the migration scripts");
    System.err.println();
    System.err.println("options can be:");
    System.err.println("  --modules <module,...>  - comm seperated list of module names");
  }

  private static String findModules(String[] args) {
    final Iterator<String> iter = Arrays.asList(args).iterator();
    while (iter.hasNext()) {
      final String arg = iter.next();
      if (arg.startsWith("--modules=")) return arg.substring(10);
      if (arg.equals("--modules") && iter.hasNext()) return iter.next();
    }
    return "";
  }

  private static void process(String[] args) {
    if (args.length == 0) {
      usage();
      return;
    }

    final String action = args[0];
    final String modules = findModules(args);

    System.setProperty("axelor.task.database", action);
    System.setProperty("axelor.task.database.modules", modules);

    if ("init".equals(action)) {
      init();
    } else if ("update".equals(action)) {
      update();
    } else if ("encrypt".equals(action)) {
      encrypt();
    } else if ("migrate".equals(action)) {
      migrate();
    } else {
      usage();
    }
  }

  public static void main(String[] args) {
    AppLogger.install();
    try {
      process(args);
    } finally {
      AppLogger.uninstall();
    }
  }
}
