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

public final class IdeaHelper {

  public static void createLauncher(
      File rootDir, String project, List<String> args, List<String> vmArgs) {

    final String name = String.format("%s (run)", project);
    final String outName = String.format("%s.xml", name.replaceAll("[^\\w]", "_"));
    final File outFile = FileUtils.getFile(rootDir, ".idea", "runConfigurations", outName);
    final String code =
        "<component name='ProjectRunConfigurationManager'>\n"
            + generateRunConfiguration(name, project, args, vmArgs)
            + "</component>\n";

    try {
      Files.createParentDirs(outFile);
      Files.asCharSink(outFile, Charsets.UTF_8).write(code);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String generateRunConfiguration(
      String name, String project, List<String> args, List<String> vmArgs) {
    final StringBuilder builder = new StringBuilder();
    builder
        .append("<configuration default=\"false\" name=\"")
        .append(name)
        .append("\" type=\"Application\" factoryName=\"Application\" singleton=\"true\">\n");
    builder.append(
        "  <extension name=\"coverage\" enabled=\"false\" merge=\"false\" sample_coverage=\"true\" runner=\"idea\" />\n");
    builder.append(
        "  <option name=\"MAIN_CLASS_NAME\" value=\"com.axelor.app.internal.AppRunner\" />\n");
    builder
        .append("  <option name=\"VM_PARAMETERS\" value=\"")
        .append(Joiner.on(" ").join(vmArgs))
        .append("\" />\n");
    builder
        .append("  <option name=\"PROGRAM_PARAMETERS\" value=\"")
        .append(Joiner.on(" ").join(args))
        .append("\" />\n");
    builder.append("  <option name=\"WORKING_DIRECTORY\" value=\"file://$PROJECT_DIR$\" />\n");
    builder.append("  <option name=\"ALTERNATIVE_JRE_PATH_ENABLED\" value=\"false\" />\n");
    builder.append("  <option name=\"ALTERNATIVE_JRE_PATH\" />\n");
    builder.append("  <option name=\"ENABLE_SWING_INSPECTOR\" value=\"false\" />\n");
    builder.append("  <option name=\"ENV_VARIABLES\" />\n");
    builder.append("  <option name=\"PASS_PARENT_ENVS\" value=\"true\" />\n");
    builder.append("  <shortenClasspath name=\"MANIFEST\" />\n");
    builder.append("  <module name=\"").append(project).append(".main").append("\" />\n");
    builder.append("  <envs />\n");
    builder.append("  <method>\n");
    builder.append("    <option name=\"Make\" enabled=\"true\" />\n");
    builder.append("  </method>\n");
    builder.append("</configuration>\n");
    return builder.toString();
  }
}
