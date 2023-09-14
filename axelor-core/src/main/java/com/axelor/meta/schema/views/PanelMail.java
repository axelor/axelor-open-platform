/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

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
