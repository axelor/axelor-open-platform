/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2020 Axelor (<http://axelor.com>).
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
package com.axelor.tools.ide;

import com.axelor.common.FileUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class EclipseHelper {

  public static void createLauncher(
      File rootDir, String project, List<String> args, List<String> vmArgs) {
    final StringBuilder builder = new StringBuilder();
    builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
    builder.append(
        "<launchConfiguration type=\"org.eclipse.jdt.launching.localJavaApplication\">\n");

    builder
        .append("<listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_PATHS\">\n")
        .append("<listEntry value=\"/")
        .append(project)
        .append("\"/>\n")
        .append("</listAttribute>\n");

    builder
        .append("<listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_TYPES\">\n")
        .append("<listEntry value=\"4\"/>\n")
        .append("</listAttribute>\n");

    builder.append(
        "<booleanAttribute key=\"org.eclipse.jdt.launching.ATTR_EXCLUDE_TEST_CODE\" value=\"true\"/>\n");
    builder.append(
        "<booleanAttribute key=\"org.eclipse.jdt.launching.ATTR_USE_CLASSPATH_ONLY_JAR\" value=\"true\"/>\n");
    builder.append(
        "<booleanAttribute key=\"org.eclipse.jdt.launching.ATTR_USE_START_ON_FIRST_THREAD\" value=\"true\"/>\n");

    builder
        .append("<stringAttribute key=\"org.eclipse.jdt.launching.MAIN_TYPE\"")
        .append(" value=")
        .append('"')
        .append("com.axelor.app.internal.AppRunner")
        .append('"')
        .append("/>\n");

    builder
        .append("<stringAttribute key=\"org.eclipse.jdt.launching.PROGRAM_ARGUMENTS\"")
        .append(" value=")
        .append('"')
        .append(Joiner.on(' ').join(args))
        .append('"')
        .append("/>\n");

    builder
        .append("<stringAttribute key=\"org.eclipse.jdt.launching.PROJECT_ATTR\"")
        .append(" value=")
        .append('"')
        .append(project)
        .append('"')
        .append("/>\n");

    builder
        .append("<stringAttribute key=\"org.eclipse.jdt.launching.VM_ARGUMENTS\"")
        .append(" value=")
        .append('"')
        .append(Joiner.on(' ').join(vmArgs))
        .append('"')
        .append("/>\n");

    builder.append("</launchConfiguration>");

    final String outName = String.format("%s (run).launch", project);
    final File output = FileUtils.getFile(rootDir, ".settings", outName);
    try {
      Files.createParentDirs(output);
      Files.asCharSink(output, Charsets.UTF_8).write(builder);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
