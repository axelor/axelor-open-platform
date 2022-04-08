package com.axelor.app;

import static com.axelor.app.AvailableAppSettings.APPLICATION_MODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.TestingHelpers;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AppSettingGetPropTest {

  @BeforeAll
  static void setup() {
    TestingHelpers.resetSettings();
  }

  @AfterEach
  void tearDown() {
    TestingHelpers.resetSettings();
  }

  // Test with settings defined in src/test/resources/axelor-config.properties

  @Test
  public void shouldGetOrDefault() {
    AppSettings settings = AppSettings.get();
    settings.getInternalProperties().put("empty", "");

    // null prop
    assertNull(settings.get("unexisting"));
    assertEquals("default", settings.get("unexisting", "default"));

    // empty prop
    assertEquals("", settings.get("empty"));
    assertEquals("default", settings.get("empty", "default"));

    // existing prop
    assertEquals("Tests", settings.get("application.name", "default"));
  }

  @Test
  public void shouldGetBoolean() {
    AppSettings settings = AppSettings.get();

    assertFalse(settings.getBoolean("unexisting", false));
    assertTrue(settings.getBoolean("unexisting", true));
    assertTrue(settings.getBoolean("quartz.enable", false));
  }

  @Test
  public void shouldGetInt() {
    AppSettings settings = AppSettings.get();

    assertEquals(50, settings.getInt("unexisting", 50));
    assertEquals(60, settings.getInt("session.timeout", 50));
  }

  @Test
  public void shouldGetList() {
    AppSettings settings = AppSettings.get();
    settings.getInternalProperties().put("empty", "");
    settings.getInternalProperties().put("oneElement", "a");
    settings.getInternalProperties().put("myList", "a,b,c");

    assertTrue(settings.getList("unexisting").isEmpty());
    assertTrue(settings.getList("empty").isEmpty());
    assertEquals(List.of("a"), settings.getList("oneElement"));
    assertEquals(List.of("a", "b", "c"), settings.getList("myList"));

    assertTrue(settings.getList("unexisting", String::toUpperCase).isEmpty());
    assertEquals(List.of("A", "B", "C"), settings.getList("myList", String::toUpperCase));
  }

  @Test
  public void shouldGetSub() {
    AppSettings settings = AppSettings.get();
    settings.getInternalProperties().put("aYear", "{year}Me");
    settings.getInternalProperties().put("aMonth", "{month}Me");
    settings.getInternalProperties().put("fullDate", "{day}{month}{year}");

    final LocalDate now = LocalDate.now();

    assertEquals(String.valueOf(now.getYear()) + "Me", settings.get("aYear"));
    assertEquals(String.valueOf(now.getMonthValue()) + "Me", settings.get("aMonth"));
    assertEquals(
        String.valueOf(now.getDayOfMonth()) + now.getMonthValue() + now.getYear(),
        settings.get("fullDate"));
  }

  @Test
  public void shouldGetIsProduction() {
    AppSettings settings = AppSettings.get();
    assertFalse(settings.isProduction());

    settings.getInternalProperties().put(APPLICATION_MODE, "test");
    assertTrue(settings.isProduction());

    settings.getInternalProperties().put(APPLICATION_MODE, "prod");
    assertTrue(settings.isProduction());
  }
}
