package com.axelor.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

public abstract class AbstractMapTester {

  void assertMap(Map<String, String> map) {
    assertEquals("James", map.get("test"));
    assertNull(map.get("name.sub"));
    assertEquals("Foo", map.get("name.sub.my-foo"));
    assertEquals("Bar", map.get("name.sub.my-bar"));
    assertNull(map.get("ignore"));
    assertEquals("one", map.get("list[0]"));
    assertEquals("two", map.get("list[1]"));
  }
}
