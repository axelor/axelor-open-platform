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

import com.axelor.events.PostAction;
import com.axelor.meta.ActionHandler;
import com.axelor.rpc.ActionResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import java.util.regex.Pattern;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlType(name = "AbstractAction")
public abstract class Action {

  protected final transient Logger log = LoggerFactory.getLogger(getClass());

  @XmlAttribute(name = "id")
  private String xmlId;

  @XmlTransient @JsonProperty private Long actionId;

  @XmlAttribute private String name;

  @XmlAttribute private String model;

  @XmlAttribute(name = "if-module")
  private String moduleToCheck;

  public String getXmlId() {
    return xmlId;
  }

  public void setXmlId(String xmlId) {
    this.xmlId = xmlId;
  }

  public Long getActionId() {
    return actionId;
  }

  public void setActionId(Long actionId) {
    this.actionId = actionId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getModuleToCheck() {
    return moduleToCheck;
  }

  public void setModuleToCheck(String moduleToCheck) {
    this.moduleToCheck = moduleToCheck;
  }

  public Object execute(ActionHandler handler) {
    handler.firePreEvent(getName());
    Object result = evaluate(handler);
    PostAction event = handler.firePostEvent(getName(), result);
    return event.getResult();
  }

  public Object wrap(ActionHandler handler) {
    Object result = execute(handler);
    return result instanceof ActionResponse ? result : wrapper(result);
  }

  protected Object wrapper(Object value) {
    return value;
  }

  protected abstract Object evaluate(ActionHandler handler);

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass()).add("name", getName()).toString();
  }

  static String toExpression(String expression, boolean quote) {
    Pattern pattern = Pattern.compile("^(#\\{|(eval|select|action):)");
    if (expression != null && !pattern.matcher(expression).find()) {
      expression = "eval: " + (quote ? "\"\"\"" + expression + "\"\"\"" : expression);
    }
    return expression;
  }

  static boolean test(ActionHandler handler, String expression) {
    if (Strings.isNullOrEmpty(expression)) // if expression is not given always return true
    return true;
    if ("true".equals(expression)) return true;
    if ("false".equals(expression)) return false;
    Object result = handler.evaluate(toExpression(expression, false));
    if (result == null) return false;
    if (result instanceof Number && result.equals(0)) return false;
    if (result instanceof Boolean) return (Boolean) result;
    return true;
  }

  @XmlType
  public abstract static class Element {

    @XmlAttribute(name = "if")
    private String condition;

    @XmlAttribute private String name;

    @XmlAttribute(name = "expr")
    private String expression;

    public String getCondition() {
      return condition;
    }

    public void setCondition(String condition) {
      this.condition = condition;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getExpression() {
      return expression;
    }

    public void setExpression(String expression) {
      this.expression = expression;
    }

    boolean test(ActionHandler handler) {
      return Action.test(handler, getCondition());
    }
  }
}
