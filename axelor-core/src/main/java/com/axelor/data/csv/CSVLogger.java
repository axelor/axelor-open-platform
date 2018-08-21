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
package com.axelor.data.csv;

import com.axelor.data.adapter.DataAdapter;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This {@link CSVLogger} class logs all errors during the import into a specific file. */
public class CSVLogger {

  private static final String REMOTE_SCHEMA = "data-import_" + CSVConfig.VERSION + ".xsd";

  private static final String DEFAULT_CONFIG_NAME = "csv-config.xml";

  private Logger LOG = LoggerFactory.getLogger(getClass());

  private File errorDir;

  private CSVInput csvInput;

  private CSVConfig csvConfig;

  private String[] header;

  private File currentFile;

  private File configFile;

  private List<String> filesName = Lists.newArrayList();

  private boolean inputExported;

  /**
   * Main constructor
   *
   * @param csvConfig the config
   * @param dir that will contain the files
   */
  public CSVLogger(CSVConfig csvConfig, String dir) {
    this.errorDir = this.computeDir(dir);
    this.csvConfig = csvConfig;

    this.cleanDir(this.errorDir);
  }

  public File getCurrentFile() {
    return currentFile;
  }

  /**
   * Delete all files and directories of the directory
   *
   * @param file the directory
   */
  private void cleanDir(File file) {
    if (file.isDirectory()) {

      if (file.list().length == 0) {
        file.delete();
      } else {
        String files[] = file.list();

        for (String temp : files) {
          File fileDelete = new File(file, temp);
          cleanDir(fileDelete);
        }

        if (file.list().length == 0) {
          file.delete();
        }
      }
    } else {
      file.delete();
    }
  }

  /**
   * Get the directory where errors will be logs. Takes care to delete the directory.
   *
   * @param errorDir that will contain the files
   * @return the directory
   */
  private File computeDir(String errorDir) {
    return new File(Files.simplifyPath(errorDir));
  }

  /**
   * Log the row into a specific file.
   *
   * @param values the row values
   */
  public void log(String[] values) {
    if (this.errorDir == null || this.currentFile == null || this.csvInput == null) {
      return;
    }

    this.exportInput();
    try {
      if (!this.currentFile.exists()) {
        Files.createParentDirs(this.currentFile);
        Files.asCharSink(this.currentFile, Charsets.UTF_8, FileWriteMode.APPEND)
            .write(Joiner.on(this.csvInput.getSeparator()).join(this.transformLine(this.header)));
        this.filesName.add(this.currentFile.getName());
      }
      Files.asCharSink(this.configFile, Charsets.UTF_8, FileWriteMode.APPEND)
          .write("\n" + Joiner.on(this.csvInput.getSeparator()).join(this.transformLine(values)));
    } catch (IOException e) {
    }
  }

  /**
   * Quote all text in tab
   *
   * @param values the row values
   * @return list of the quoted row values
   */
  private Collection<String> transformLine(String[] values) {
    return Collections2.transform(
        Arrays.asList(values),
        new Function<String, String>() {

          @Override
          public String apply(String value) {
            return "\"" + value + "\"";
          }
        });
  }

  /**
   * Configures the file that will receive the rows in error
   *
   * @param csvInput the input
   * @param fields the header fields
   */
  public void prepareInput(CSVInput csvInput, String[] fields) {
    this.header = fields;
    this.csvInput = csvInput;
    this.currentFile = this.getCurrentFile(csvInput.getFileName());
    this.inputExported = false;
  }

  /**
   * Return an unique file (not already created)
   *
   * @param fileName file name
   * @return file the file
   */
  private File getCurrentFile(String fileName) {
    if (!this.filesName.contains(this.csvInput.getFileName())) {
      return new File(this.errorDir, this.csvInput.getFileName());
    }
    return new File(this.errorDir, getCurrentFile(this.csvInput.getFileName(), 1));
  }

  /**
   * Recursive method that determinate an unique file name
   *
   * @param fileName file name
   * @param level last level
   * @return unique file name
   */
  private String getCurrentFile(String fileName, int level) {
    String name =
        this.csvInput.getFileName().replace(".csv", "").concat("_" + level).concat(".csv");
    if (!this.filesName.contains(name)) {
      return name;
    }
    return getCurrentFile(fileName, level++);
  }

  /** Export the binding if not already exported */
  private void exportInput() {
    if (this.inputExported == false) {
      try {
        this.computeBindings();
      } catch (IOException ex) {
        LOG.error("Error while accessing file {}", this.configFile.getName());
      } finally {
        this.inputExported = true;
      }
    }
  }

  /**
   * Append the current binding to the config file.
   *
   * @throws IOException if unable to read config file
   */
  private void computeBindings() throws IOException {
    List<String> lines = Lists.newArrayList();
    StringBuilder sb = new StringBuilder();
    XStream xStream = new XStream();
    xStream.processAnnotations(CSVConfig.class);
    String originalFileName = this.csvInput.getFileName();

    if (configFile == null) {
      this.createConfigFile();

      for (DataAdapter adapter : this.csvConfig.getAdapters()) {
        sb.append(xStream.toXML(adapter));
      }
    } else {
      lines = Files.readLines(this.configFile, Charsets.UTF_8);

      for (int i = 0; i < lines.size() - 1; i++) {
        if (i == 0 || i == 1 || i == (lines.size() - 1)) {
          continue;
        }

        String line = lines.get(i);
        sb.append(line);
        if (!Strings.isNullOrEmpty(line)) {
          sb.append("\n");
        }
      }
    }

    this.csvInput.setFileName(this.currentFile.getName());
    sb.append(xStream.toXML(this.csvInput));
    this.csvInput.setFileName(originalFileName);

    Files.asCharSink(this.configFile, Charsets.UTF_8).write(this.prepareXML(sb.toString()));
  }

  /**
   * Create the config file using {@link DEFAULT_CONFIG_NAME} as file name
   *
   * @throws IOException if unable to create file
   */
  private void createConfigFile() throws IOException {
    this.configFile = new File(this.errorDir, DEFAULT_CONFIG_NAME);
    Files.createParentDirs(this.configFile);
  }

  /**
   * Build the xml file with appropriate declaration and name space.
   *
   * @param xml the source xml to wrap
   * @return string proper xml wrapped with root element
   */
  private String prepareXML(String xml) {
    StringBuilder sb = new StringBuilder("<?xml version='1.0' encoding='UTF-8'?>\n");
    sb.append("<csv-inputs")
        .append(" xmlns='")
        .append(CSVConfig.NAMESPACE)
        .append("'")
        .append(" xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'")
        .append(" xsi:schemaLocation='")
        .append(CSVConfig.NAMESPACE)
        .append(" ")
        .append(CSVConfig.NAMESPACE + "/" + REMOTE_SCHEMA)
        .append("'")
        .append(">\n\n")
        .append(xml)
        .append("\n\n</csv-inputs>");
    return sb.toString();
  }
}
