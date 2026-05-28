/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
