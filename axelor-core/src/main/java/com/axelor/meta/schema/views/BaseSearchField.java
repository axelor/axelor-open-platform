/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.axelor.db.JPA;
import com.axelor.db.mapper.Adapter;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@XmlType
@JsonInclude(Include.NON_NULL)
public class BaseSearchField extends Field {

  @XmlAttribute private Boolean multiple;

  @JsonGetter("type")
  @Override
  public String getServerType() {
    return super.getServerType();
  }

  public Boolean getMultiple() {
    return multiple;
  }

  public void setMultiple(Boolean multiple) {
    this.multiple = multiple;
  }

  public static Map<String, Class<?>> getTypes() {
    return TYPES;
  }

  private static final Map<String, Class<?>> TYPES =
      Map.of(
          "string", String.class,
          "integer", Integer.class,
          "decimal", BigDecimal.class,
          "date", LocalDate.class,
          "datetime", LocalDateTime.class,
          "boolean", Boolean.class);

  @SuppressWarnings("rawtypes")
  public Object validate(Object input) {
    try {
      String serverType = getServerType();
      Class<?> klass = serverType != null ? TYPES.get(serverType) : null;
      if ("reference".equals(serverType)) {
        klass = Class.forName(getTarget());
        if (input != null) {
          return JPA.em().find(klass, Long.valueOf(((Map) input).get("id").toString()));
        }
      }
      if ("enum".equals(serverType)) {
        return input;
      }
      if (klass != null && BigDecimal.class.isAssignableFrom(klass) && input == null) {
        return null;
      }
      if (klass != null) {
        return Adapter.adapt(input, klass, klass, null);
      }
    } catch (Exception e) {
      // ignore
    }
    return input;
  }
}
