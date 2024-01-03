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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.TestingHelpers;
import com.axelor.app.settings.EnvSettingSource;
import com.axelor.app.settings.PropertiesSettingsSource;
import com.axelor.app.settings.SystemSettingSource;
import com.axelor.app.settings.YamlSettingsSource;
import com.axelor.common.ClassUtils;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

public class AppSettingsTest {

  @BeforeAll
  static void setup() {
    TestingHelpers.resetSettings();
  }

  @AfterEach
  void tearDown() {
    TestingHelpers.resetSettings();
  }

  @Test
  public void shouldGetAppSettings() {
    AppSettings settings = AppSettings.get();
    assertNotNull(settings);
  }

  @Test
  public void testPropertiesMap() {
    AppSettings settings = AppSettings.get();

    // unmodifiableMap
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          settings.getProperties().put("some", "thing");
        });

    // real map
    assertDoesNotThrow(
        () -> {
          settings.getInternalProperties().put("some", "thing");
        });

    assertTrue(settings.getPropertiesKeys().size() > 3);

    assertEquals(
        Set.of("application.name", "application.description", "application.mode"),
        settings.getPropertiesKeysStartingWith("application"));
    assertEquals(3, settings.getPropertiesStartingWith("application").size());
  }

  @Nested
  class YamlSettingsSourceTest {

    @AfterEach
    void tearDown() {
      TestingHelpers.resetSettings();
    }

    @Test
    void shouldLoadEmptyYaml() {
      YamlSettingsSource source = new YamlSettingsSource((URL) null);
      assertEquals(0, source.getProperties().size());
    }

    @Test
    void shouldLoadYaml() {
      YamlSettingsSource source =
          new YamlSettingsSource(ClassUtils.getResource("configs/ext-config.yml"));
      assertEquals(5, source.getProperties().size());
      assertEquals("Foo", source.getValue("views.foo"));
      assertEquals("Bar", source.getValue("views.some-bar"));
    }
  }

  @Nested
  class PropertiesSettingsSourceTest {

    @AfterEach
    void tearDown() {
      TestingHelpers.resetSettings();
    }

    @Test
    void shouldLoadProperties() {
      PropertiesSettingsSource source =
          new PropertiesSettingsSource(ClassUtils.getResource("configs/ext-config.properties"));
      assertEquals(2, source.getProperties().size());
      assertEquals("From ext properties", source.getValue("application.name"));
      assertEquals("Bar", source.getValue("application.some-bar"));
    }
  }

  @Nested
  @ExtendWith(MyEnv.class)
  class EnvSettingsSourceTest {

    @AfterEach
    void tearDown() {
      TestingHelpers.resetSettings();
    }

    @Test
    void shouldLoadEnv() {
      EnvSettingSource source = new EnvSettingSource();
      assertEquals(2, source.getProperties().size());
      assertEquals("myEnv", source.getValue("my.env"));
      assertEquals("true", source.getValue("var"));
    }
  }

  @Nested
  class SystemSettingsSourceTest {

    private Map<String, String> props =
        Map.of("axelor.config.a.prop", "some", "axelor.config.p-bar", "thing");

    @BeforeEach
    void setup() {
      for (Map.Entry<String, String> entry : props.entrySet()) {
        System.setProperty(entry.getKey(), entry.getValue());
      }
    }

    @AfterEach
    void tearDown() {
      TestingHelpers.resetSettings();
    }

    @Test
    void shouldLoadSystem() {
      SystemSettingSource source = new SystemSettingSource();
      assertEquals(2, source.getProperties().size());
      assertEquals("some", source.getValue("a.prop"));
      assertEquals("thing", source.getValue("p-bar"));
    }
  }

  static class MyEnv extends EnvironmentVariablesExtension {
    public MyEnv() {
      super();
    }

    @Override
    public void beforeAll(ExtensionContext context) {
      super.beforeAll(context);
      set("AXELOR_CONFIG_MY_ENV", "myEnv");
      set("AXELOR_CONFIG_VAR", "true");
    }
  }
}
