/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.gradle.tasks;

import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.tools.encryption.StringEncryption;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.tasks.options.Option;

public class EncryptFileTask extends AbstractEncryptTask {

  public static final String TASK_NAME = "encryptFile";
  public static final String TASK_DESCRIPTION = "Encrypts the property values wrapped with 'ENC()'";

  private String file;
  private Pattern encryptPattern = Pattern.compile("ENC\\((.*)\\)$");
  public static final String ENCRYPT_PREFIX = "ENC(";
  public static final String ENCRYPT_SUFFIX = ")";

  private List<String> encodedSettings = new ArrayList<>();

  @Option(
      option = "file",
      description = "File to encrypt. By default it will lookup for configuration file in project.")
  public void setFile(String file) {
    this.file = file;
  }

  @Override
  public void validate() {
    super.validate();

    if (this.configurationPath == null || !this.configurationPath.toFile().exists()) {
      throw new IllegalStateException("Unable to determinate configuration file");
    }
  }

  @Override
  public void doEncrypt() {
    try {
      processLines(this.configurationPath);
    } catch (Exception e) {
      getLogger().error("", e);
    }
  }

  @Override
  public void printOutput() {
    log("");
    log("-------OUTPUT-------");
    String text = String.format("Found and encrypt %s setting(s)", encodedSettings.size());
    if (ObjectUtils.notEmpty(encodedSettings)) {
      text += " : " + String.join(", ", encodedSettings);
    }
    log(text);
  }

  private void processLines(Path file) throws IOException {
    FileProcessor processor = new FileProcessor(file, getEncryptor());
    processor.process();
    encodedSettings = processor.getEncodedSettings();
    if (ObjectUtils.isEmpty(encodedSettings)) {
      return;
    }

    Files.write(file, processor.getGeneratedLines(), StandardCharsets.UTF_8);
  }

  @Override
  protected Path getConfigurationFile() {
    if (StringUtils.isBlank(this.file)) {
      return super.getConfigurationFile();
    }
    return Paths.get(this.file);
  }

  class FileProcessor {

    private final Path file;
    private final StringEncryption encryptor;
    private final List<String> finalLines = new ArrayList<>();
    private final List<String> encodedSettings = new ArrayList<>();

    public FileProcessor(Path file, StringEncryption encryptor) {
      this.file = file;
      this.encryptor = encryptor;
    }

    public List<String> getEncodedSettings() {
      return this.encodedSettings;
    }

    public List<String> getGeneratedLines() {
      return this.finalLines;
    }

    public void process() throws IOException {
      for (String line : Files.readAllLines(this.file, StandardCharsets.UTF_8)) {
        processLine(line);
      }
    }

    private void processLine(String line) {
      Matcher matcher = encryptPattern.matcher(line);
      if (matcher.find()) {
        String extractedValue = matcher.group(1);
        if (ObjectUtils.notEmpty(extractedValue)) {
          String matchGroup = matcher.group();
          addLine(
              line.replace(matchGroup, getEncodedSettings(extractedValue)), extractSetting(line));
          return;
        }
      }

      addLine(line, null);
    }

    private String extractSetting(String line) {
      line = line.substring(0, line.indexOf(ENCRYPT_PREFIX)).trim();
      if (line.endsWith(":")) {
        line = line.substring(0, line.lastIndexOf(':')).trim();
      } else {
        line = line.substring(0, line.lastIndexOf('=')).trim();
      }
      return line;
    }

    private void addLine(String line, String setting) {
      finalLines.add(line);
      if (ObjectUtils.notEmpty(setting)) {
        encodedSettings.add(setting);
      }
    }

    private CharSequence getEncodedSettings(String value) {
      return ENCRYPT_PREFIX + encryptor.encrypt(value) + ENCRYPT_SUFFIX;
    }
  }
}
