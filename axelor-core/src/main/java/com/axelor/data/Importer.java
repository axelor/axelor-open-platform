/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.data;

import com.axelor.data.adapter.BooleanAdapter;
import com.axelor.data.adapter.DataAdapter;
import com.axelor.data.adapter.JavaTimeAdapter;
import com.axelor.data.adapter.NumberAdapter;
import com.axelor.data.adapter.PasswordAdapter;
import com.axelor.data.csv.CSVImporter;
import com.google.inject.ImplementedBy;
import java.util.Map;

/** The {@link Importer} interface to run data import. */
@ImplementedBy(CSVImporter.class)
public interface Importer {

  public static DataAdapter[] defaultAdapters = {
    new DataAdapter(
        "LocalDate", JavaTimeAdapter.class, "type", "LocalDate", "format", "dd/MM/yyyy"),
    new DataAdapter("LocalTime", JavaTimeAdapter.class, "type", "LocalTime", "format", "HH:mm"),
    new DataAdapter(
        "LocalDateTime",
        JavaTimeAdapter.class,
        "type",
        "LocalDateTime",
        "format",
        "dd/MM/yyyy HH:mm"),
    new DataAdapter(
        "ZonedDateTime",
        JavaTimeAdapter.class,
        "type",
        "ZonedDateTime",
        "format",
        "dd/MM/yyyy HH:mm"),
    new DataAdapter("Boolean", BooleanAdapter.class, "falsePattern", "(0|f|n|false|no)"),
    new DataAdapter(
        "Number", NumberAdapter.class, "decimalSeparator", ".", "thousandSeparator", ","),
    new DataAdapter("Password", PasswordAdapter.class)
  };

  /**
   * Set global context.
   *
   * @param context the global context
   */
  void setContext(Map<String, Object> context);

  /**
   * Add a data import event listener.
   *
   * @param listener the listener
   */
  void addListener(Listener listener);

  /** Clear listeners. */
  void clearListener();

  /** Run the data import task. */
  void run();

  /**
   * Run the specified import task.
   *
   * @param task the task to run
   */
  void run(ImportTask task);
}
