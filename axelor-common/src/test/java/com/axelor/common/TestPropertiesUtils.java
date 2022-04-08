package com.axelor.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;

public class TestPropertiesUtils extends AbstractMapTester {

  @Test
  public void wrongFileTest() throws IOException {
    Properties prop = PropertiesUtils.loadProperties(ResourceUtils.getResource("wrong.properties"));
    assertTrue(prop.isEmpty());
  }

  @Test
  public void loadProperties() throws IOException {
    Properties prop = PropertiesUtils.loadProperties(ResourceUtils.getResource("test.properties"));
    assertEquals(5, prop.size());
  }

  @Test
  public void toMapTest() throws IOException {
    Properties prop = PropertiesUtils.loadProperties(ResourceUtils.getResource("test.properties"));
    assertMap(PropertiesUtils.propertiesToMap(prop));
  }
}
