/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tomcat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.FileResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.coyote.AbstractProtocol;

public class TomcatServer {

  private Tomcat tomcat;

  private final TomcatOptions options;

  public TomcatServer(TomcatOptions options) {
    this.options = options;
  }

  private Tomcat create() throws IOException {
    final Path baseDir = options.getBaseDir();
    final Path logsDir = baseDir.resolve("logs");

    Files.createDirectories(logsDir);

    final int port = options.getPort();
    final String contextPath = options.getContextPath();
    final String docBase = options.getDocBase().toFile().getAbsolutePath();

    final Tomcat tomcat = new Tomcat();

    tomcat.setBaseDir(baseDir.toFile().getAbsolutePath());
    tomcat.getHost().setAutoDeploy(false);

    final Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
    connector.setPort(port);
    connector.setProperty("bindOnInit", "false");

    if (options.getMaxThreads() > 0
        && connector.getProtocolHandler() instanceof AbstractProtocol<?> p) {
      p.setMaxThreads(options.getMaxThreads());
    }

    final URI proxyUri = options.getProxyUrl();
    if (proxyUri != null) {
      String scheme = proxyUri.getScheme();
      int proxyPort = proxyUri.getPort();
      if (proxyPort <= 0) {
        proxyPort = scheme.equals("https") ? 443 : 80;
      }
      connector.setProxyName(proxyUri.getHost());
      connector.setScheme(scheme);
      if (scheme.equals("https")) {
        connector.setSecure(true);
      }
      connector.setProxyPort(proxyPort);
    }

    tomcat.setConnector(connector);
    tomcat.setPort(port);

    final StandardContext context = (StandardContext) tomcat.addWebapp(contextPath, docBase);
    final StandardRoot resources = new StandardRoot();

    resources.setCacheMaxSize(options.getCacheMaxSize());

    context.setParentClassLoader(getClass().getClassLoader());
    context.setResources(resources);
    context.setUnpackWAR(false);

    // additional webapp resources
    options.getExtraResources().stream()
        .map(Path::toFile)
        .map(dir -> new DirResourceSet(resources, "/", dir.getAbsolutePath(), "/"))
        .forEach(resources::addPostResources);

    // additional classes, should be search before libs
    options.getClasses().stream()
        .map(Path::toFile)
        .map(dir -> new DirResourceSet(resources, "/WEB-INF/classes", dir.getAbsolutePath(), "/"))
        .forEach(resources::addPreResources);

    // additional libs
    options.getLibs().stream()
        .map(Path::toFile)
        .map(
            file ->
                new FileResourceSet(
                    resources, "/WEB-INF/lib/" + file.getName(), file.getAbsolutePath(), "/"))
        .forEach(resources::addPostResources);

    tomcat
        .getServer()
        .addLifecycleListener(
            new LifecycleListener() {
              @Override
              public void lifecycleEvent(LifecycleEvent event) {
                final Lifecycle lifecycle = event.getLifecycle();
                if (lifecycle.getState() == LifecycleState.STARTED) {
                  tomcat.getServer().removeLifecycleListener(this);
                  System.out.println();
                  System.out.println("Running at http://localhost:" + port + contextPath);
                  System.out.println();
                }
              }
            });

    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

    return tomcat;
  }

  public void start() {
    try {
      if (tomcat == null) {
        tomcat = create();
      }
      tomcat.start();
    } catch (Exception e) {
      throw new RuntimeException("Cannot start Tomcat " + e.getMessage(), e);
    }
    if (tomcat != null) {
      tomcat.getServer().await();
    }
  }

  public void stop() {
    if (tomcat == null) {
      return;
    }
    try {
      tomcat.stop();
      tomcat = null;
    } catch (Exception e) {
      throw new RuntimeException("Cannot Stop Tomcat " + e.getMessage(), e);
    }
  }
}
