package com.axelor.app.settings;

import com.axelor.common.StringUtils;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class YamlSettingsSource extends AbstractSettingsSource {

  private static final Logger LOG = LoggerFactory.getLogger(YamlSettingsSource.class);

  public YamlSettingsSource(URL url) {
    this(loadYaml(url));
  }

  public YamlSettingsSource(Map<String, Object> values) {
    super(getFlattenedMap(values));
  }

  private static Map<String, Object> loadYaml(URL resource) {
    if (resource == null) {
      return new HashMap<>();
    }

    try (InputStream stream = resource.openStream()) {
      Yaml yaml = new Yaml();
      return yaml.load(stream);
    } catch (Exception e) {
      LOG.trace("Unable to open {} yaml file", resource);
    }

    return new HashMap<>();
  }

  /**
   * Flatten {@link Map} into a flat {@link Map} with key names using property dot notation.
   *
   * @param source {@link Map} to flattern
   * @return result flattened {@link Map}
   */
  private static Map<String, String> getFlattenedMap(Map<String, Object> source) {
    Map<String, String> result = new LinkedHashMap<>();
    flattenMap(result, source.entrySet().iterator(), "");
    return result;
  }

  private static void flattenMap(
      Map<String, String> result, Iterator<Map.Entry<String, Object>> source, String prefix) {
    if (StringUtils.notBlank(prefix)) {
      prefix = prefix + ".";
    }

    while (source.hasNext()) {
      Map.Entry<String, Object> entry = source.next();
      flattenElement(result, entry.getValue(), prefix.concat(entry.getKey()));
    }
  }

  @SuppressWarnings("unchecked")
  private static void flattenElement(Map<String, String> result, Object source, String prefix) {
    if (source instanceof Iterable) {
      flattenCollection(result, (Iterable<Object>) source, prefix);
      return;
    }

    if (source instanceof Map) {
      flattenMap(result, ((Map<String, Object>) source).entrySet().iterator(), prefix);
      return;
    }

    result.put(prefix, source == null ? null : source.toString());
  }

  private static void flattenCollection(
      Map<String, String> result, Iterable<Object> iterable, String prefix) {
    int counter = 0;

    for (Object element : iterable) {
      flattenElement(result, element, prefix + "[" + counter + "]");
      counter++;
    }
  }
}
