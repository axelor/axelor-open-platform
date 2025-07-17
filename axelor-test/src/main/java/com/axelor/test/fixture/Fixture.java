/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.test.fixture;

import com.axelor.common.ResourceUtils;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Fixture class to load YAML fixtures for JPA entities.
 *
 * <p>This class reads YAML files from the classpath, maps them to Java objects, and allows for
 * persistence of those objects.
 *
 * <p>Example YAML fixture:
 *
 * <pre>
 * - type: Country
 *   key: country:france
 *   properties:
 *     name: France
 *     code: FR
 *
 * - type: Country
 *   key: country:usa
 *   properties:
 *       name: United States
 *       code: US
 *
 * - type: Address
 *   key: address:home
 *   properties:
 *       street: 123 Main St
 *       city: Paris
 *       postalCode: 75001
 *       country: country:france
 *
 * - type: Address
 *   key: address:work
 *   properties:
 *       street: 456 Elm St
 *       city: New York
 *       postalCode: 10001
 *       country: country:usa
 *
 * - type: Person
 *   key: person:john_doe
 *   properties:
 *     firstName: John
 *     lastName: Doe
 *     email: john.doe@example.com
 *     addresses:
 *       - address:home
 *       - address:work
 * </pre>
 */
public class Fixture {

  private static final String PROP_TYPE = "type";
  private static final String PORP_KEY = "key";
  private static final String PROP_PROPERTIES = "properties";

  private InputStream readStream(String fixture) throws IOException {
    var stream = ResourceUtils.getResourceStream(fixture);
    if (stream == null) {
      // Try to load from fixtures directory
      stream = ResourceUtils.getResourceStream("fixtures/" + fixture);
    }
    if (stream == null) {
      throw new IOException("Fixture file not found: " + fixture);
    }
    return stream;
  }

  public List<Object> load(
      String fixture, Function<String, Class<?>> resolve, Function<Object, Object> persist)
      throws IOException {

    var mapper = new FixtureMapper();
    var options = new LoaderOptions();
    var yaml = new Yaml(options);

    try (var stream = readStream(fixture)) {
      var records = yaml.loadAs(stream, List.class);
      var beans = new ArrayList<Object>();

      // Phase 1: Create beans
      for (var record : records) {
        var map = (Map<?, ?>) record;
        var key = (String) map.get(PORP_KEY);
        var type = (String) map.get(PROP_TYPE);
        var beanType = resolve.apply(type);
        if (beanType == null) throw new IllegalArgumentException("Unknown type: " + type);
        if (key == null || key.isBlank()) {
          throw new IllegalArgumentException("Missing key for type: " + type);
        }

        var values = (Map<?, ?>) map.get(PROP_PROPERTIES);
        if (values == null || values.isEmpty()) {
          throw new IllegalArgumentException("Missing properties for type: " + type);
        }

        var bean = mapper.map(beanType, key, Map.of());
        if (bean == null) {
          throw new IllegalArgumentException("Failed to create bean for type: " + type);
        }

        beans.add(bean);
      }

      // Phase 2: Set properties
      for (var record : records) {
        var map = (Map<?, ?>) record;
        var key = (String) map.get(PORP_KEY);
        var type = (String) map.get(PROP_TYPE);
        var beanType = resolve.apply(type);
        var values = (Map<?, ?>) map.get(PROP_PROPERTIES);

        mapper.map(beanType, key, values);
      }

      // Phase 3: Persist beans in reverse order
      for (var bean : Lists.reverse(beans)) {
        persist.apply(bean);
      }

      return beans;
    }
  }
}
