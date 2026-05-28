/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tomcat;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TomcatOptions {

  private int port = 8080;

  private String contextPath = "";

  private URI proxyUrl;

  private Path baseDir;

  private int maxThreads;

  private int cacheMaxSize = 100 * 1024; // 100M

  private List<Path> roots = new ArrayList<>();

  private List<Path> classes = new ArrayList<>();

  private List<Path> libs = new ArrayList<>();

  private Set<String> tldScanJars = new LinkedHashSet<>();

  // axelor-web-*.jar is included by default so that @ServerEndpoint (and other
  // Servlet 3.0 pluggability) classes shipped in axelor-web are discovered.
  private Set<String> pluggabilityScanJars = new LinkedHashSet<>(List.of("axelor-web-*.jar"));

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

  public TomcatOptions addTldScanJar(String pattern) {
    tldScanJars.add(pattern);
    return this;
  }

  public TomcatOptions addPluggabilityScanJar(String pattern) {
    pluggabilityScanJars.add(pattern);
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

  public URI getProxyUrl() {
    return proxyUrl;
  }

  public void setProxyUrl(URI proxyUrl) {
    this.proxyUrl = proxyUrl;
  }

  public void setContextPath(String contextPath) {
    String context =
        Optional.ofNullable(contextPath)
            .map(String::trim)
            .map(x -> x.startsWith("/") ? x : "/" + x)
            .map(x -> x.equals("/") ? "" : x)
            .orElse("");
    this.contextPath = context;
  }

  public Path getBaseDir() {
    return baseDir == null ? Path.of("build/tomcat") : baseDir;
  }

  public void setBaseDir(Path baseDir) {
    this.baseDir = baseDir;
  }

  public int getMaxThreads() {
    return maxThreads;
  }

  public void setMaxThreads(int maxThreads) {
    this.maxThreads = maxThreads;
  }

  public int getCacheMaxSize() {
    return cacheMaxSize;
  }

  public void setCacheMaxSize(int cacheMaxSize) {
    this.cacheMaxSize = cacheMaxSize;
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

  public Set<String> getTldScanJars() {
    return tldScanJars;
  }

  public Set<String> getPluggabilityScanJars() {
    return pluggabilityScanJars;
  }

  public Path getDocBase() {
    return roots.isEmpty() ? Path.of("src/main/webapp") : roots.getFirst();
  }

  public List<Path> getExtraResources() {
    return roots.isEmpty() ? roots : roots.stream().skip(1).collect(Collectors.toList());
  }
}
