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

import com.axelor.common.PropertiesUtils;
import com.axelor.common.StringUtils;
import com.axelor.common.YamlUtils;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.tools.encryption.StringEncryption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

public abstract class AbstractEncryptTask extends DefaultTask {

  public static final String TASK_GROUP = AxelorPlugin.AXELOR_BUILD_GROUP;

  public static final String CONFIG_ENCRYPTOR_PREFIX = "config.encryptor";

  public static final String PASSWORD_KEY = "password";
  public static final String ALGORITHM_KEY = "algorithm";
  public static final String KEY_OBTENTION_ITERATIONS_KEY = "key-obtention-iterations";
  public static final String PROVIDER_NAME_KEY = "provider-name";
  public static final String PROVIDER_CLASS_NAME_KEY = "provider-class-name";
  public static final String SALT_GENERATOR_CLASSNAME_KEY = "salt-generator-classname";
  public static final String IV_GENERATOR_CLASSNAME_KEY = "iv-generator-classname";
  public static final String STRING_OUTPUT_TYPE_KEY = "string-output-type";

  protected String password;
  protected String algorithm = null;
  protected String keyObtentionIterations = null;
  protected String providerName = null;
  protected String providerClassName = null;
  protected String saltGeneratorClassname = null;
  protected String ivGeneratorClassname = null;
  protected String stringOutputType = null;

  private static final String CONFIGS_FILES_PATH = "src/main/resources";
  private static final List<String> CONFIGS_FILES =
      List.of("axelor-config.properties", "axelor-config.yml", "axelor-config.yaml");

  protected Path configurationPath;

  private StringEncryption encryptor;

  @Option(option = PASSWORD_KEY, description = "Master Password")
  public void setPassword(String password) {
    this.password = password;
  }

  @Option(option = ALGORITHM_KEY, description = "Algorithm to be used by the encryptor")
  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  @Option(
      option = KEY_OBTENTION_ITERATIONS_KEY,
      description = "Number of hashing iterations to obtain the signing key")
  public void setKeyObtentionIterations(String keyObtentionIterations) {
    this.keyObtentionIterations = keyObtentionIterations;
  }

  @Option(
      option = PROVIDER_NAME_KEY,
      description = "Name of the security provider to be asked for the encryption algorithm")
  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  @Option(
      option = PROVIDER_CLASS_NAME_KEY,
      description = "Security provider to be used for obtaining the encryption algorithm")
  public void setProviderClassName(String providerClassName) {
    this.providerClassName = providerClassName;
  }

  @Option(
      option = SALT_GENERATOR_CLASSNAME_KEY,
      description = "Salt generator to be used by the encryptor")
  public void setSaltGeneratorClassname(String saltGeneratorClassname) {
    this.saltGeneratorClassname = saltGeneratorClassname;
  }

  @Option(
      option = IV_GENERATOR_CLASSNAME_KEY,
      description = "IV generator to be used by the encryptor")
  public void setIvGeneratorClassname(String ivGeneratorClassname) {
    this.ivGeneratorClassname = ivGeneratorClassname;
  }

  @Option(
      option = STRING_OUTPUT_TYPE_KEY,
      description = "Sets the form in which String output will be encoded")
  public void setStringOutputType(String stringOutputType) {
    this.stringOutputType = stringOutputType;
  }

  public void validate() {
    if (StringUtils.isBlank(this.password)) {
      throw new IllegalStateException("--password is required");
    }
  }

  public abstract void doEncrypt();

  public abstract void printOutput();

  public void end() {
    printConfigs();
    printOutput();
  }

  @TaskAction
  public void encrypt() {
    lookupEncryptionSettings();
    validate();
    doEncrypt();
    end();
  }

  protected void log(String message, Object... args) {
    getLogger().lifecycle(message, args);
  }

  @Internal
  protected StringEncryption getEncryptor() {
    if (this.encryptor == null) {
      encryptor =
          new StringEncryption(
              this.password,
              this.algorithm,
              this.keyObtentionIterations,
              this.providerName,
              this.providerClassName,
              this.saltGeneratorClassname,
              this.ivGeneratorClassname,
              this.stringOutputType);
    }
    return this.encryptor;
  }

  private void lookupEncryptionSettings() {
    this.configurationPath = getConfigurationFile();
    if (this.configurationPath == null) {
      return;
    }
    Map<String, String> properties = parseProperties(this.configurationPath);
    assignDefaultProperties(
        properties.entrySet().stream()
            .filter(e -> e.getKey().startsWith(CONFIG_ENCRYPTOR_PREFIX))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  private void assignDefaultProperties(Map<String, String> props) {
    set(this::setAlgorithm, this.algorithm, props, ALGORITHM_KEY);
    set(
        this::setKeyObtentionIterations,
        this.keyObtentionIterations,
        props,
        KEY_OBTENTION_ITERATIONS_KEY);
    set(this::setProviderName, this.providerName, props, PROVIDER_NAME_KEY);
    set(this::setProviderClassName, this.providerClassName, props, PROVIDER_CLASS_NAME_KEY);
    set(
        this::setSaltGeneratorClassname,
        this.saltGeneratorClassname,
        props,
        SALT_GENERATOR_CLASSNAME_KEY);
    set(
        this::setIvGeneratorClassname,
        this.ivGeneratorClassname,
        props,
        IV_GENERATOR_CLASSNAME_KEY);
    set(this::setStringOutputType, this.stringOutputType, props, STRING_OUTPUT_TYPE_KEY);
  }

  private Map<String, String> parseProperties(Path path) {
    try {
      if (path.toString().endsWith(".properties")) {
        return PropertiesUtils.propertiesToMap(PropertiesUtils.loadProperties(path));
      } else {
        return YamlUtils.getFlattenedMap(YamlUtils.loadYaml(path));
      }
    } catch (Exception e) {
      getLogger().error(String.format("Unable to open configuration file %s", path));
    }
    return new HashMap<>();
  }

  @Internal
  protected Path getConfigurationFile() {
    Path rootPath = Paths.get(getProject().getRootDir().toURI());
    for (String fileName : CONFIGS_FILES) {
      Path filePath = rootPath.resolve(CONFIGS_FILES_PATH).resolve(fileName);
      if (filePath.toFile().exists()) {
        log("Found configuration file {}", fileName);
        return filePath;
      }
    }

    return null;
  }

  private void set(
      Consumer<String> setter, String cliValue, Map<String, String> props, String key) {
    String val = extractProp(props, key);
    if (StringUtils.notBlank(cliValue) || StringUtils.isBlank(val)) {
      return;
    }
    setter.accept(val);
  }

  private String extractProp(Map<String, String> props, String key) {
    return props.get(CONFIG_ENCRYPTOR_PREFIX + "." + key);
  }

  private void printConfigs() {
    log("");
    log("-------Configs-------");
    printConfig(this.password, PASSWORD_KEY);
    printConfig(this.algorithm, ALGORITHM_KEY);
    printConfig(this.keyObtentionIterations, KEY_OBTENTION_ITERATIONS_KEY);
    printConfig(this.providerName, PROVIDER_NAME_KEY);
    printConfig(this.providerClassName, PROVIDER_CLASS_NAME_KEY);
    printConfig(this.saltGeneratorClassname, SALT_GENERATOR_CLASSNAME_KEY);
    printConfig(this.ivGeneratorClassname, IV_GENERATOR_CLASSNAME_KEY);
    printConfig(this.stringOutputType, STRING_OUTPUT_TYPE_KEY);
    log("");
    getLogger()
        .lifecycle(
            String.format(
                "WARNING : Do not add property `%s.%s` with the password in your configuration file.\n"
                    + "Use a reference to an external file : `file:<path_to_file>` as password value.",
                CONFIG_ENCRYPTOR_PREFIX, PASSWORD_KEY));
  }

  private void printConfig(String value, String key) {
    if (StringUtils.isBlank(value)) {
      return;
    }
    log(String.format("%s.%s = %s", CONFIG_ENCRYPTOR_PREFIX, key, value));
  }
}
