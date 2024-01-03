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
package com.axelor.app.settings;

import com.axelor.common.StringUtils;
import java.util.Map;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

public class StringEncryptorBuilder {

  private final EncryptorConfigurationProperties encryptorConfigurationProperties;

  public StringEncryptorBuilder(Map<String, String> props) {
    this(new EncryptorConfigurationProperties(props));
  }

  public StringEncryptorBuilder(EncryptorConfigurationProperties encryptorConfigurationProperties) {
    this.encryptorConfigurationProperties = encryptorConfigurationProperties;
  }

  public StringEncryptor build() {
    return createEncryptor();
  }

  private StringEncryptor createEncryptor() {
    StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
    SimpleStringPBEConfig config = new SimpleStringPBEConfig();
    if (StringUtils.isBlank(encryptorConfigurationProperties.getPassword())) {
      throw new IllegalStateException("Encryption password is required");
    }
    config.setPassword(encryptorConfigurationProperties.getPassword());
    config.setAlgorithm(encryptorConfigurationProperties.getAlgorithm());
    config.setKeyObtentionIterations(encryptorConfigurationProperties.getKeyObtentionIterations());
    config.setProviderName(encryptorConfigurationProperties.getProviderName());
    config.setProviderClassName(encryptorConfigurationProperties.getProviderClassName());
    config.setSaltGeneratorClassName(encryptorConfigurationProperties.getSaltGeneratorClassname());
    config.setIvGeneratorClassName(encryptorConfigurationProperties.getIvGeneratorClassname());
    config.setStringOutputType(encryptorConfigurationProperties.getStringOutputType());
    encryptor.setConfig(config);
    return encryptor;
  }
}
