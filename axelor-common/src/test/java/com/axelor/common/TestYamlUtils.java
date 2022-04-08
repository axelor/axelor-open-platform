package com.axelor.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TestYamlUtils extends AbstractMapTester {

  @Test
  public void wrongFileTest() throws IOException {
    Map<String, Object> prop = YamlUtils.loadYaml(ResourceUtils.getResource("wrong.properties"));
    assertTrue(prop.isEmpty());
  }

  @Test
  public void loadYaml() throws IOException {
    Map<String, Object> prop = YamlUtils.loadYaml(ResourceUtils.getResource("test.yaml"));
    assertEquals(3, prop.size());
  }

  @Test
  public void toMapTest() throws IOException {
    Map<String, Object> prop = YamlUtils.loadYaml(ResourceUtils.getResource("test.yaml"));
    assertEquals(5, YamlUtils.getFlattenedMap(prop).size());
    assertMap(YamlUtils.getFlattenedMap(prop));
  }
}
