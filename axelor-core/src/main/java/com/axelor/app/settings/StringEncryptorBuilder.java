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
