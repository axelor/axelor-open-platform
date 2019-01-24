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
package com.axelor.gradle;

import com.axelor.common.StringUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HotswapExtension {

  public static final String EXTENSION_NAME = "hotswapAgent";

  private File logFile;

  private Boolean logAppend;

  private Map<String, String> loggers = new LinkedHashMap<>();

  private List<String> disabledPlugins = new ArrayList<>();

  private List<File> extraClasspath = new ArrayList<>();

  private List<File> watchResources = new ArrayList<>();

  public Map<String, String> getLoggers() {
    return loggers;
  }

  public void setLoggers(Map<String, String> loggers) {
    this.loggers = loggers;
  }

  public File getLogFile() {
    return logFile;
  }

  public void setLogFile(File logFile) {
    this.logFile = logFile;
  }

  public Boolean getLogAppend() {
    return logAppend;
  }

  public void setLogAppend(Boolean logAppend) {
    this.logAppend = logAppend;
  }

  public List<String> getDisabledPlugins() {
    return disabledPlugins;
  }

  public void setDisabledPlugins(List<String> disabledPlugins) {
    this.disabledPlugins = disabledPlugins;
  }

  public List<File> getExtraClasspath() {
    return extraClasspath;
  }

  public void setExtraClasspath(List<File> extraClasspath) {
    this.extraClasspath = extraClasspath;
  }

  public List<File> getWatchResources() {
    return watchResources;
  }

  public void setWatchResources(List<File> watchResources) {
    this.watchResources = watchResources;
  }

  public void disablePlugin(String plugin) {
    disabledPlugins.add(plugin);
  }

  public void logger(String name, String level) {
    final String logger = StringUtils.isBlank(name) ? "LOGGER" : "LOGGER." + name;
    loggers.put(logger, level);
  }
}
