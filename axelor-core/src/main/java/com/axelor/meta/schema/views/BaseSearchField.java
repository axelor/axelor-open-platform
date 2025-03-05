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
      Class<?> klass = TYPES.get(getServerType());
      if ("reference".equals(getServerType())) {
        klass = Class.forName(getTarget());
        if (input != null) {
          return JPA.em().find(klass, Long.valueOf(((Map) input).get("id").toString()));
        }
      }
      if ("enum".equals(getServerType())) {
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
