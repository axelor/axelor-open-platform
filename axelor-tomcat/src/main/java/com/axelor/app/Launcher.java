/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.app;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Launcher {

  private static URL toURL(File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static Path getWebapp() {
    try {
      return Path.of(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI())
          .getParent()
          .getParent()
          .resolve("app")
          .normalize()
          .toAbsolutePath();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Cannot find webapp", e);
    }
  }

  public static void main(String[] args) {

    ClassLoader loader = Launcher.class.getClassLoader();

    Path webapp = getWebapp();
    Path webinf = webapp.resolve("WEB-INF");

    List<URL> paths = new ArrayList<>();

    paths.add(toURL(webinf.resolve("classes").toFile()));

    for (File lib : webinf.resolve("lib").toFile().listFiles()) {
      if (lib.getName().endsWith(".jar")) {
        paths.add(toURL(lib));
      }
    }

    try (URLClassLoader appLoader = new URLClassLoader(paths.toArray(URL[]::new), loader)) {
      Thread.currentThread().setContextClassLoader(appLoader);
      Class<?> type = appLoader.loadClass("com.axelor.app.cli.CliRunner");
      Method main = type.getMethod("main", String[].class);
      Object[] params = {args};
      main.invoke(null, params);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      Thread.currentThread().setContextClassLoader(loader);
    }
  }
}
