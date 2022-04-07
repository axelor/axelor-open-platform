package com.axelor.app.settings;

import static com.axelor.app.settings.SettingsUtils.ENCRYPT_PROPS_PREFIX;

import com.axelor.common.StringUtils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

public class EncryptorConfigurationProperties {

  /** Master Password */
  private String password;

  /** Algorithm to be used by the encryptor */
  private String algorithm = "PBEWITHHMACSHA512ANDAES_256";

  /** Number of hashing iterations to obtain the signing key */
  private String keyObtentionIterations = null;

  /** Name of the security provider to be asked for the encryption algorithm */
  private String providerName = null;

  /** Security provider to be used for obtaining the encryption algorithm */
  private String providerClassName = null;

  /** Salt generator to be used by the encryptor */
  private String saltGeneratorClassname = null;

  /** IV generator to be used by the encryptor */
  private String ivGeneratorClassname = "org.jasypt.iv.RandomIvGenerator";

  /** Sets the form in which String output will be encoded */
  private String stringOutputType = "base64";

  public EncryptorConfigurationProperties(Map<String, String> props) {
    init(props);
  }

  private void init(Map<String, String> props) {
    extractPassword(props, "password");
    set(this::setAlgorithm, props, "algorithm");
    set(this::setKeyObtentionIterations, props, "key-obtention-iterations");
    set(this::setProviderName, props, "provider-name");
    set(this::setProviderClassName, props, "provider-class-name");
    set(this::setSaltGeneratorClassname, props, "salt-generator-classname");
    set(this::setIvGeneratorClassname, props, "iv-generator-classname");
    set(this::setStringOutputType, props, "string-output-type");
  }

  private void extractPassword(Map<String, String> props, String key) {
    String password = extractProp(props, key);
    if (StringUtils.isBlank(password)) {
      return;
    }
    if (password.startsWith("file:")) {
      URI resource = URI.create(password);
      try (BufferedReader br = new BufferedReader(new FileReader(resource.toURL().getFile()))) {
        password = br.readLine();
      } catch (Exception e) {
        // ignore
      }
    }
    setPassword(password);
  }

  private void set(Consumer<String> setter, Map<String, String> props, String key) {
    String val = extractProp(props, key);
    if (StringUtils.isBlank(val)) {
      return;
    }
    setter.accept(val);
  }

  private String extractProp(Map<String, String> props, String key) {
    return props.get(ENCRYPT_PROPS_PREFIX + "." + key);
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  public String getKeyObtentionIterations() {
    return keyObtentionIterations;
  }

  public void setKeyObtentionIterations(String keyObtentionIterations) {
    this.keyObtentionIterations = keyObtentionIterations;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public String getProviderClassName() {
    return providerClassName;
  }

  public void setProviderClassName(String providerClassName) {
    this.providerClassName = providerClassName;
  }

  public String getSaltGeneratorClassname() {
    return saltGeneratorClassname;
  }

  public void setSaltGeneratorClassname(String saltGeneratorClassname) {
    this.saltGeneratorClassname = saltGeneratorClassname;
  }

  public String getIvGeneratorClassname() {
    return ivGeneratorClassname;
  }

  public void setIvGeneratorClassname(String ivGeneratorClassname) {
    this.ivGeneratorClassname = ivGeneratorClassname;
  }

  public String getStringOutputType() {
    return stringOutputType;
  }

  public void setStringOutputType(String stringOutputType) {
    this.stringOutputType = stringOutputType;
  }
}
