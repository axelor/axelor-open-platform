/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.text;

import com.axelor.db.EntityHelper;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.MetaStore;
import com.axelor.script.ScriptAllowed;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import groovy.xml.XmlUtil;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ScriptAllowed
class FormatHelper {

  private final Logger log = LoggerFactory.getLogger(FormatHelper.class);

  public Object escape(Object value) {
    if (value == null) {
      return "";
    }
    return XmlUtil.escapeXml(value.toString());
  }

  public String text(Object bean, String expr) {
    if (bean == null) {
      return "";
    }
    expr = expr.replaceAll("\\?", "");
    return getTitle(EntityHelper.getEntityClass(bean), expr, getValue(bean, expr));
  }

  private String getTitle(Class<?> klass, String expr, Object value) {
    if (value == null) {
      return "";
    }
    final Property property = this.getProperty(klass, expr);
    final String val = value == null ? "" : value.toString();
    try {
      return MetaStore.getSelectionItem(property.getSelection(), val).getLocalizedTitle();
    } catch (Exception e) {
    }
    return val;
  }

  private Property getProperty(Class<?> beanClass, String name) {
    Iterator<String> iter = Splitter.on(".").split(name).iterator();
    Property p = Mapper.of(beanClass).getProperty(iter.next());
    while (iter.hasNext() && p != null) {
      p = Mapper.of(p.getTarget()).getProperty(iter.next());
    }
    return p;
  }

  @SuppressWarnings("all")
  private Object getValue(Object bean, String expr) {
    if (bean == null) return null;
    Iterator<String> iter = Splitter.on(".").split(expr).iterator();
    Object obj = null;
    if (bean instanceof Map map) {
      obj = map.get(iter.next());
    } else {
      obj = Mapper.of(EntityHelper.getEntityClass(bean)).get(bean, iter.next());
    }
    if (iter.hasNext() && obj != null) {
      return getValue(obj, Joiner.on(".").join(iter));
    }
    return obj;
  }

  public void info(String text, Object... params) {
    log.info(text, params);
  }

  public void debug(String text, Object... params) {
    log.debug(text, params);
  }

  public void error(String text, Object... params) {
    log.error(text, params);
  }

  public void trace(String text, Object... params) {
    log.trace(text, params);
  }
}
