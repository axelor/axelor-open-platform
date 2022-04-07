package com.axelor.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axelor.common.ClassUtils;
import com.axelor.common.ResourceUtils;
import java.lang.reflect.Field;
import java.net.URL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class EncryptedSettingsTest {

  @Nested
  class EncryptWithDefaultAlgorithmTest {

    @BeforeEach
    void setup() {
      System.setProperty(
          "axelor.config.my.password",
          "ENC(t6TImm9F8ysWPXTvO07+ivdEo9MqKk1rdPHzXvNyfvU1dAuFfAVy/B2CehDJEeB409I9OzWyQMPRLYSq8jfMAQ==)");
      System.setProperty("axelor.config.props.encryptor.password", "MySecureKey");
    }

    @AfterEach
    void tearDown() {
      resetSettings();
    }

    @Test
    public void test() {
      AppSettings settings = AppSettings.get();

      assertEquals("A secret to encode", settings.get("my.password"));
    }
  }

  @Nested
  class EncryptWithCustomAlgorithmTest {

    @BeforeEach
    void setup() {
      String file = ClassUtils.getResource("configs/test-encrypted.properties").getFile();
      System.setProperty("axelor.config", file);

      System.setProperty("axelor.config.props.encryptor.password", "MySecureKey");
    }

    @AfterEach
    void tearDown() {
      resetSettings();
    }

    @Test
    public void test() {
      AppSettings settings = AppSettings.get();

      assertEquals("A secret to encode", settings.get("my.secret"));
    }
  }

  @Nested
  class EncryptWithExternalPasswordTest {

    @BeforeEach
    void setup() {
      URL resource = ResourceUtils.getResource("configs/test-encryptor-key");
      System.setProperty(
          "axelor.config.my.password",
          "ENC(t6TImm9F8ysWPXTvO07+ivdEo9MqKk1rdPHzXvNyfvU1dAuFfAVy/B2CehDJEeB409I9OzWyQMPRLYSq8jfMAQ==)");
      System.setProperty("axelor.config.props.encryptor.password", resource.toString());
    }

    @AfterEach
    void tearDown() {
      resetSettings();
    }

    @Test
    public void test() {
      AppSettings settings = AppSettings.get();

      assertEquals("A secret to encode", settings.get("my.password"));
    }
  }

  @Nested
  class NoEncryptionPasswordTest {

    @BeforeEach
    void setup() {
      System.setProperty(
          "axelor.config.my.password",
          "ENC(t6TImm9F8ysWPXTvO07+ivdEo9MqKk1rdPHzXvNyfvU1dAuFfAVy/B2CehDJEeB409I9OzWyQMPRLYSq8jfMAQ==)");
    }

    @AfterEach
    void tearDown() {
      resetSettings();
    }

    @Test
    public void test() {
      assertThrows(RuntimeException.class, AppSettings::get);
    }
  }

  protected static void resetSettings() {
    System.getProperties().stringPropertyNames().stream()
        .filter(it -> it.startsWith("axelor.config"))
        .forEach(System::clearProperty);

    try {
      Field instance = AppSettings.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, null);
    } catch (Exception e) {
      throw new RuntimeException();
    }
  }
}
