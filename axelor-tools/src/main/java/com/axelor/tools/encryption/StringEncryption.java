/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.tools.encryption;

import com.axelor.common.StringUtils;
import java.util.function.Consumer;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

public class StringEncryption {

  private String password;
  private String algorithm = "PBEWITHHMACSHA512ANDAES_256";
  private String keyObtentionIterations;
  private String providerName;
  private String providerClassName;
  private String saltGeneratorClassname;
  private String ivGeneratorClassname = "org.jasypt.iv.RandomIvGenerator";
  private String stringOutputType;

  private PBEStringEncryptor encryptor;

  public StringEncryption(
      String password,
      String algorithm,
      String keyObtentionIterations,
      String providerName,
      String providerClassName,
      String saltGeneratorClassname,
      String ivGeneratorClassname,
      String stringOutputType) {
    set(this::setPassword, password);
    set(this::setAlgorithm, algorithm);
    set(this::setKeyObtentionIterations, keyObtentionIterations);
    set(this::setProviderName, providerName);
    set(this::setProviderClassName, providerClassName);
    set(this::setSaltGeneratorClassname, saltGeneratorClassname);
    set(this::setIvGeneratorClassname, ivGeneratorClassname);
    set(this::setStringOutputType, stringOutputType);
    encryptor = createEncryptor();
  }

  private void set(Consumer<String> consumer, String value) {
    if (StringUtils.isBlank(value)) {
      return;
    }
    consumer.accept(value);
  }

  public String encrypt(String message) {
    return encryptor.encrypt(message);
  }

  PBEStringEncryptor createEncryptor() {
    StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
    SimpleStringPBEConfig config = new SimpleStringPBEConfig();
    if (StringUtils.isBlank(this.password)) {
      throw new IllegalStateException("Encryption password is required.");
    }
    config.setPassword(this.password);
    config.setAlgorithm(this.algorithm);
    config.setKeyObtentionIterations(this.keyObtentionIterations);
    config.setProviderName(this.providerName);
    config.setProviderClassName(this.providerClassName);
    config.setSaltGeneratorClassName(this.saltGeneratorClassname);
    config.setIvGeneratorClassName(this.ivGeneratorClassname);
    config.setStringOutputType(this.stringOutputType);
    encryptor.setConfig(config);
    return encryptor;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  public void setKeyObtentionIterations(String keyObtentionIterations) {
    this.keyObtentionIterations = keyObtentionIterations;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public void setProviderClassName(String providerClassName) {
    this.providerClassName = providerClassName;
  }

  public void setSaltGeneratorClassname(String saltGeneratorClassname) {
    this.saltGeneratorClassname = saltGeneratorClassname;
  }

  public void setIvGeneratorClassname(String ivGeneratorClassname) {
    this.ivGeneratorClassname = ivGeneratorClassname;
  }

  public void setStringOutputType(String stringOutputType) {
    this.stringOutputType = stringOutputType;
  }
}
