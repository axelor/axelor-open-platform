/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.gradle.tasks;

import com.axelor.common.StringUtils;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.tools.encryption.StringEncryption;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

public class EncryptTextTask extends DefaultTask {

  public static final String TASK_NAME = "encryptText";
  public static final String TASK_DESCRIPTION = "Encrypt the given text.";
  public static final String TASK_GROUP = AxelorPlugin.AXELOR_BUILD_GROUP;

  public static final String ENCRYPT_PROPS_PREFIX = "props.encryptor";

  public static final String PASSWORD_KEY = "password";
  public static final String ALGORITHM_KEY = "algorithm";
  public static final String KEY_OBTENTION_ITERATIONS_KEY = "key-obtention-iterations";
  public static final String PROVIDER_NAME_KEY = "provider-name";
  public static final String PROVIDER_CLASS_NAME_KEY = "provider-class-name";
  public static final String SALT_GENERATOR_CLASSNAME_KEY = "salt-generator-classname";
  public static final String IV_GENERATOR_CLASSNAME_KEY = "iv-generator-classname";
  public static final String STRING_OUTPUT_TYPE_KEY = "string-output-type";

  private String text;

  private String password;
  private String algorithm = null;
  private String keyObtentionIterations = null;
  private String providerName = null;
  private String providerClassName = null;
  private String saltGeneratorClassname = null;
  private String ivGeneratorClassname = null;
  private String stringOutputType = null;

  @Option(option = "text", description = "String to encrypt")
  public void setText(String text) {
    this.text = text;
  }

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

  @TaskAction
  public void encrypt() {
    if (StringUtils.isBlank(this.text)) {
      throw new IllegalStateException("--text is required");
    }
    if (StringUtils.isBlank(this.password)) {
      throw new IllegalStateException("--password is required");
    }
    String encryptedText =
        new StringEncryption(
                this.password,
                this.algorithm,
                this.keyObtentionIterations,
                this.providerName,
                this.providerClassName,
                this.saltGeneratorClassname,
                this.ivGeneratorClassname,
                this.stringOutputType)
            .encrypt(this.text);

    printConfigs();
    printOutput(encryptedText);
  }

  private void printConfigs() {
    getLogger().lifecycle("");
    getLogger().lifecycle("-------Configs-------");
    printConfig(this.password, PASSWORD_KEY);
    printConfig(this.algorithm, ALGORITHM_KEY);
    printConfig(this.keyObtentionIterations, KEY_OBTENTION_ITERATIONS_KEY);
    printConfig(this.providerName, PROVIDER_NAME_KEY);
    printConfig(this.providerClassName, PROVIDER_CLASS_NAME_KEY);
    printConfig(this.saltGeneratorClassname, SALT_GENERATOR_CLASSNAME_KEY);
    printConfig(this.ivGeneratorClassname, IV_GENERATOR_CLASSNAME_KEY);
    printConfig(this.stringOutputType, STRING_OUTPUT_TYPE_KEY);
    getLogger().lifecycle("");
    getLogger()
        .lifecycle(
            String.format(
                "WARNING : Do not add property `%s.%s` with the password in your configuration file.\n"
                    + "Use a reference to an external file : `file:<path_to_file>` as password value.",
                ENCRYPT_PROPS_PREFIX, PASSWORD_KEY));
  }

  private void printConfig(String value, String key) {
    if (StringUtils.isBlank(value)) {
      return;
    }
    getLogger().lifecycle(String.format("%s.%s = %s", ENCRYPT_PROPS_PREFIX, key, value));
  }

  private void printOutput(String encryptedText) {
    getLogger().lifecycle("");
    getLogger().lifecycle("-------OUTPUT-------");
    getLogger().lifecycle(encryptedText);
  }
}
