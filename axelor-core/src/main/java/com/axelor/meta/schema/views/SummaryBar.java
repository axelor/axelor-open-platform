/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.axelor.i18n.I18n;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SummaryBar {

  @XmlAttribute private String hint;

  @XmlElement(name = "field")
  private List<SummaryField> items;

  @XmlElement private SummaryCall call;

  public String getHint() {
    return hint;
  }

  public void setHint(String hint) {
    this.hint = hint;
  }

  public List<SummaryField> getItems() {
    return items;
  }

  public void setItems(List<SummaryField> items) {
    this.items = items;
  }

  public SummaryCall getCall() {
    return call;
  }

  public void setCall(SummaryCall call) {
    this.call = call;
  }

  @XmlType
  public static class SummaryField extends SummaryBaseField {

    @XmlAttribute private String aggregate;

    @XmlAttribute private String on;

    public String getAggregate() {
      return aggregate;
    }

    public void setAggregate(String aggregate) {
      this.aggregate = aggregate;
    }

    public String getOn() {
      return on;
    }

    public void setOn(String on) {
      this.on = on;
    }
  }

  @XmlType
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class SummaryCall {

    @XmlElement(name = "field")
    private List<SummaryCallField> items;

    @XmlAttribute private String action;

    public List<SummaryCallField> getItems() {
      return items;
    }

    public void setItems(List<SummaryCallField> items) {
      this.items = items;
    }

    public String getAction() {
      return action;
    }

    public void setAction(String action) {
      this.action = action;
    }
  }

  @XmlType
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class SummaryCallField extends SummaryBaseField {
    @XmlAttribute(name = "type")
    private String serverType;

    public String getServerType() {
      return serverType;
    }

    public void setServerType(String serverType) {
      this.serverType = serverType;
    }
  }

  @XmlType
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class SummaryBaseField {
    @XmlAttribute private String name;
    @XmlAttribute private String title;
    @XmlAttribute private String align;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    @JsonGetter("title")
    public String getLocalizedTitle() {
      return I18n.get(title);
    }

    @JsonIgnore
    public String getTitle() {
      return title;
    }

    @JsonSetter
    public void setTitle(String title) {
      this.title = title;
    }

    public String getAlign() {
      return align;
    }

    public void setAlign(String align) {
      this.align = align;
    }
  }
}
