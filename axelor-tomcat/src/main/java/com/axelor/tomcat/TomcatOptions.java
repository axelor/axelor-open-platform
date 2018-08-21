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
package com.axelor.tomcat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TomcatOptions {

  private int port = 8080;

  private String contextPath = "/";

  private Path baseDir;

  private List<Path> roots = new ArrayList<>();

  private List<Path> classes = new ArrayList<>();

  private List<Path> libs = new ArrayList<>();

  public TomcatOptions(Path webapp) {
    this.roots.add(webapp);
  }

  public TomcatOptions(List<Path> webapps) {
    this.roots.addAll(webapps);
  }

  public TomcatOptions addWebapp(Path path) {
    roots.add(path);
    return this;
  }

  public TomcatOptions addClasses(Path path) {
    classes.add(path);
    return this;
  }

  public TomcatOptions addLib(Path path) {
    libs.add(path);
    return this;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getContextPath() {
    return contextPath;
  }

  public void setContextPath(String contextPath) {
    String context = contextPath == null ? "" : contextPath.trim();
    if (context.isEmpty()) {
      context = "/";
    }
    if (context.charAt(0) != '/') {
      context = "/" + context;
    }
    this.contextPath = context;
  }

  public Path getBaseDir() {
    return baseDir == null ? Paths.get("build/tomcat") : baseDir;
  }

  public void setBaseDir(Path baseDir) {
    this.baseDir = baseDir;
  }

  public List<Path> getRoots() {
    return roots;
  }

  public List<Path> getClasses() {
    return classes;
  }

  public List<Path> getLibs() {
    return libs;
  }

  public Path getDocBase() {
    return roots.isEmpty() ? Paths.get("src/main/webapp") : roots.get(0);
  }

  public List<Path> getExtraResources() {
    return roots.isEmpty() ? roots : roots.stream().skip(1).collect(Collectors.toList());
  }
}
