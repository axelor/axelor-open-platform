/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@JsonTypeName("panel-mail")
public class PanelMail extends AbstractPanel {

  @XmlElements({
    @XmlElement(name = "mail-messages", type = MailMessages.class),
    @XmlElement(name = "mail-followers", type = MailFollowers.class)
  })
  private List<AbstractWidget> items;

  public List<AbstractWidget> getItems() {
    return process(items);
  }

  public void setItems(List<AbstractWidget> items) {
    this.items = items;
  }

  @XmlType
  @JsonTypeName("mail-messages")
  public static class MailMessages extends AbstractWidget {

    @XmlAttribute private String filter;

    @XmlAttribute private Integer limit;

    @JsonGetter("filter")
    public String getFilter() {
      return "all".equals(filter) ? null : filter;
    }

    public void setFilter(String filter) {
      this.filter = filter;
    }

    public Integer getLimit() {
      return limit;
    }

    public void setLimit(Integer limit) {
      this.limit = limit;
    }
  }

  @XmlType
  @JsonTypeName("mail-followers")
  public static class MailFollowers extends AbstractWidget {}
}
