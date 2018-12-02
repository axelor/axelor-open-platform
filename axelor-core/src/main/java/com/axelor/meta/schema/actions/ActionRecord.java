/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.meta.schema.actions;

import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.ActionHandler;
import com.axelor.rpc.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class ActionRecord extends Action {

  @XmlType
  public static class RecordField extends Element {

    @XmlAttribute(name = "copy")
    private Boolean canCopy;

    public Boolean getCanCopy() {
      return canCopy;
    }
  }

  @XmlAttribute private String search;

  @XmlAttribute private String ref;

  @XmlAttribute(name = "copy")
  private Boolean canCopy;

  @XmlAttribute(name = "saveIf")
  private String saveIf;

  @XmlElement(name = "field")
  private List<RecordField> fields;

  public String getSearch() {
    return search;
  }

  public void setSearch(String search) {
    this.search = search;
  }

  public String getRef() {
    return ref;
  }

  public void setRef(String ref) {
    this.ref = ref;
  }

  public Boolean getCanCopy() {
    return canCopy;
  }

  public void setCanCopy(Boolean canCopy) {
    this.canCopy = canCopy;
  }

  public String getSaveIf() {
    return saveIf;
  }

  public void setSaveIf(String saveIf) {
    this.saveIf = saveIf;
  }

  public List<RecordField> getFields() {
    return fields;
  }

  public void setFields(List<RecordField> field) {
    this.fields = field;
  }

  private Class<?> findClass(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object wrap(ActionHandler handler) {
    handler.firePreEvent(getName());
    Map<String, Object> map = new HashMap<>();
    Object value = evaluate(handler, map);
    handler.firePostEvent(getName(), value);
    return value == null ? null : wrapper(map);
  }

  @Override
  protected Object evaluate(ActionHandler handler) {
    return evaluate(handler, new HashMap<>());
  }

  @Override
  protected Object wrapper(Object value) {
    Map<String, Object> result = new HashMap<>();
    result.put("values", value);
    return result;
  }

  private Object evaluate(final ActionHandler handler, final Map<String, Object> map) {

    final Class<?> entityClass = findClass(getModel());
    if (ref != null) {
      Object result = handler.evaluate(ref);
      if (result != null) {
        if (canCopy == Boolean.TRUE) {
          return JPA.copy((Model) result, true);
        }
        return result;
      }
    }

    final Mapper mapper = Mapper.of(entityClass);
    final Object target = Mapper.toBean(entityClass, null);

    for (RecordField recordField : fields) {

      if (!recordField.test(handler) || StringUtils.isBlank(recordField.getName())) {
        continue;
      }

      for (String name : recordField.getName().split(",")) {
        name = name.trim();

        String expr = recordField.getExpression();
        Object value = expr;

        try {
          value = handler.evaluate(expr);
        } catch (Exception e) {
          log.error("error evaluating expression");
          log.error("expression: {}", expr, e);
          continue;
        }

        Property property = mapper.getProperty(name);
        if (property == null) { // assume dummy field
          map.put(name, value);
          continue;
        }

        if (((RecordField) recordField).getCanCopy() == Boolean.TRUE && value instanceof Model) {
          value = JPA.copy((Model) value, true);
        }

        try {
          property.set(target, value);
          if (map != null) {
            map.put(property.getName(), property.get(target));
          }
        } catch (Exception e) {
          log.error("invalid value for field: {}", property.getName());
          log.error("value: {}", value);
          continue;
        }
      }
    }

    if (search != null) {
      Object result = handler.search(entityClass, search, map);
      if (result != null) {
        if (canCopy == Boolean.TRUE) {
          return JPA.copy((Model) result, true);
        }
        return result;
      }
    }

    final Object[] result = {target};

    if (canSave(handler, (Model) target)) {
      JPA.runInTransaction(
          new Runnable() {
            @Override
            public void run() {
              Model bean = JPA.save(((Model) result[0]));
              map.putAll(Resource.toMapCompact(bean));
              result[0] = bean;
            }
          });
    }

    return result[0];
  }

  private boolean canSave(ActionHandler handler, Model bean) {
    if (bean == null || StringUtils.isBlank(saveIf)) return false;
    if (bean.getId() != null && bean.getVersion() == null) return false;
    return Action.test(handler, saveIf);
  }
}
