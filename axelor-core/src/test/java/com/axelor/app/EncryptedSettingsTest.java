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
package com.axelor.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axelor.TestingHelpers;
import com.axelor.common.ClassUtils;
import com.axelor.common.ResourceUtils;
import java.net.URL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class EncryptedSettingsTest {

  @BeforeAll
  static void setup() {
    TestingHelpers.resetSettings();
  }

  @Nested
  class EncryptWithDefaultAlgorithmTest {

    @BeforeEach
    void setup() {
      System.setProperty(
          "axelor.config.my.password",
          "ENC(t6TImm9F8ysWPXTvO07+ivdEo9MqKk1rdPHzXvNyfvU1dAuFfAVy/B2CehDJEeB409I9OzWyQMPRLYSq8jfMAQ==)");
      System.setProperty("axelor.config.config.encryptor.password", "MySecureKey");
    }

    @AfterEach
    void tearDown() {
      TestingHelpers.resetSettings();
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

      System.setProperty("axelor.config.config.encryptor.password", "MySecureKey");
    }

    @AfterEach
    void tearDown() {
      TestingHelpers.resetSettings();
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
      System.setProperty("axelor.config.config.encryptor.password", resource.toString());
    }

    @AfterEach
    void tearDown() {
      TestingHelpers.resetSettings();
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
      TestingHelpers.resetSettings();
    }

    @Test
    public void test() {
      assertThrows(RuntimeException.class, AppSettings::get);
    }
  }
}
