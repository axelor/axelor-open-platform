/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.app.cli.commands;

import com.axelor.app.cli.CliCommand;
import com.axelor.common.ClassUtils;
import com.axelor.common.StringUtils;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "run", description = "Run the application.")
public class RunCommand implements CliCommand {

  @Option(names = "--port", description = "The tomcat port nunber.", defaultValue = "8080")
  private Integer port;

  @Option(names = "--base-dir", description = "The tomcat base directory.")
  private Path baseDir;

  @Option(names = "--context-path", description = "The webapp context path.")
  private String contextPath;

  @Option(names = "--proxy-url", description = "Set proxy URL if running behind a reverse proxy.")
  private URI proxyUrl;

  @Option(names = "--max-threads", description = "Set the maximum number of worker threads.")
  private int maxThreads;

  @Option(names = "--help", usageHelp = true, hidden = true)
  private boolean help;

  @Override
  public void run() {
    try {
      ClassLoader loader = ClassUtils.getContextClassLoader();
      Class<?> type = loader.loadClass("com.axelor.tomcat.TomcatRunner");
      Method main = type.getMethod("main", String[].class);

      Path appDir =
          Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI())
              .getParent()
              .getParent()
              .normalize()
              .toAbsolutePath();

      Path webappDir = appDir.resolve("app");
      Path tomcatDir = appDir.resolve("tomcat");

      String port = Optional.ofNullable(this.port).orElse(8080).toString();
      String base = Optional.ofNullable(this.baseDir).orElse(tomcatDir).toString();

      List<String> args = new ArrayList<>();

      args.add("--port");
      args.add(port);
      args.add("--base-dir");
      args.add(base);

      if (StringUtils.notBlank(contextPath)) {
        args.add("--context-path");
        args.add(contextPath);
      }

      if (proxyUrl != null) {
        args.add("--proxy-url");
        args.add(proxyUrl.toString());
      }

      if (maxThreads > 0) {
        args.add("--max-threads");
        args.add(String.valueOf(maxThreads));
      }

      // add webapp
      args.add(webappDir.toString());

      Object[] params = {args.toArray(String[]::new)};

      main.invoke(null, params);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
