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
package com.axelor.tools.code.entity.model;

import static com.axelor.tools.code.entity.model.Utils.*;
import static com.axelor.tools.code.entity.model.Utils.isFalse;
import static com.axelor.tools.code.entity.model.Utils.isTrue;
import static com.axelor.tools.code.entity.model.Utils.list;
import static com.axelor.tools.code.entity.model.Utils.notBlank;

import com.axelor.tools.code.JavaCode;
import com.axelor.tools.code.JavaCodeUtils;
import com.axelor.tools.code.JavaMethod;
import com.axelor.tools.code.JavaParam;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class Finder {

  private static final Map<String, String> TYPES = new HashMap<>();

  static {
    TYPES.put("int", "int");
    TYPES.put("long", "long");
    TYPES.put("double", "double");
    TYPES.put("boolean", "boolean");
    TYPES.put("Integer", "Integer");
    TYPES.put("Long", "Long");
    TYPES.put("Double", "Double");
    TYPES.put("Boolean", "Boolean");
    TYPES.put("String", "String");
    TYPES.put("LocalDate", "java.time.LocalDate");
    TYPES.put("LocalTime", "java.time.LocalTime");
    TYPES.put("LocalDateTime", "java.time.LocalDateTime");
    TYPES.put("ZonedDateTime", "java.time.ZonedDateTime");
    TYPES.put("BigDecimal", "java.math.BigDecimal");
  }

  public Finder() {}

  Finder(String field) {
    this.name = methodName("findBy", field);
    this.using = field;
  }

  @XmlAttribute(name = "name", required = true)
  private String name;

  @XmlAttribute(name = "using", required = true)
  private String using;

  @XmlAttribute(name = "filter")
  private String filter;

  @XmlAttribute(name = "orderBy")
  private String orderBy;

  @XmlAttribute(name = "all")
  private Boolean all;

  @XmlAttribute(name = "cacheable")
  private Boolean cacheable;

  @XmlAttribute(name = "flush")
  private Boolean flush;

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public String getUsing() {
    return using;
  }

  public void setUsing(String value) {
    this.using = value;
  }

  public String getFilter() {
    return filter;
  }

  public void setFilter(String value) {
    this.filter = value;
  }

  public String getOrderBy() {
    return orderBy;
  }

  public void setOrderBy(String value) {
    this.orderBy = value;
  }

  public Boolean getAll() {
    if (all == null) {
      return false;
    } else {
      return all;
    }
  }

  public void setAll(Boolean value) {
    this.all = value;
  }

  public Boolean getCacheable() {
    if (cacheable == null) {
      return false;
    } else {
      return cacheable;
    }
  }

  public void setCacheable(Boolean value) {
    this.cacheable = value;
  }

  public Boolean getFlush() {
    if (flush == null) {
      return true;
    } else {
      return flush;
    }
  }

  public void setFlush(Boolean value) {
    this.flush = value;
  }

  public JavaMethod toJavaMethod(Entity entity) {
    int modifiers = Modifier.PUBLIC;
    String finderName = name;
    String finderType = entity.getName(); // entity name

    if (isTrue(all)) {
      finderType = "com.axelor.db.Query<" + finderType + ">";
    }

    JavaMethod method = new JavaMethod(finderName, finderType, modifiers);

    List<String> query = new ArrayList<>();
    List<String> args = new ArrayList<>();
    List<JavaParam> params = new ArrayList<>();

    for (String field : list(using)) {
      String[] parts = field.split(":");
      String propName = JavaCodeUtils.firstLower(field);
      String propType;
      Property prop;
      if (parts.length > 1) {
        if (filter.isEmpty()) return null; // filter must be provided
        propType = parts[0];
        propName = parts[1];
        if (TYPES.containsKey(propType)) {
          propType = TYPES.get(propType);
        } else {
          propType = parts[0];
        }
      } else {
        prop = entity.findField(propName);
        if (prop == null && entity.baseEntity != null) prop = entity.baseEntity.findField(propName);
        if (prop == null) return null;
        propType = prop.getJavaType();

        if (notBlank(prop.getTarget())) {
          propType =
              prop.getTarget().indexOf('.') == -1
                  ? entity.getPackageName() + "." + propType
                  : prop.getTarget();
        }
        query.add(String.format("self.%s = :%s", propName, propName));
      }
      propName = JavaCodeUtils.firstLower(propName);

      args.add(propName);
      params.add(new JavaParam(propName, propType));
    }

    params.forEach(method::param);

    String queryString = isBlank(filter) ? String.join(" AND ", query) : filter;

    List<JavaCode> code = new ArrayList<>();

    code.add(new JavaCode("return {0:t}.of({1:t}.class)", "com.axelor.db.Query", entity.getName()));
    code.add(new JavaCode("  .filter({0:s})", queryString));

    args.forEach(n -> code.add(new JavaCode("  .bind({0:s}, {0:l})", n)));
    list(orderBy).forEach(n -> code.add(new JavaCode("  .order({0:s})", n)));

    if (isTrue(cacheable)) {
      code.add(new JavaCode("  .cacheable()"));
    }
    if (isFalse(flush)) {
      code.add(new JavaCode("  .autoFlush(false)"));
    }

    if (finderType.equals(entity.getName())) {
      code.add(new JavaCode("  .fetchOne()"));
    }

    code.get(code.size() - 1).next(";");

    return method.code(code);
  }
}
