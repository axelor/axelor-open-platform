/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.axelor.common.StringUtils;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.persistence.oxm.annotations.XmlCDATA;

@XmlType
@JsonTypeName("calendar")
public class CalendarView extends AbstractView implements ContainerView {

  @XmlAttribute private String mode;

  @XmlAttribute private String colorBy;

  @XmlAttribute private String onChange;

  @XmlAttribute private String onDelete;

  @XmlAttribute private String eventStart;

  @XmlAttribute private String eventStop;

  @XmlAttribute private Integer eventLength;

  @XmlAttribute private Integer dayLength;

  @XmlAttribute private Boolean canDelete;

  @XmlAttribute private Boolean canNew;

  @XmlElement(name = "hilite")
  private List<CalendarEventHilite> hilites;

  @XmlElement(name = "field", type = PanelField.class)
  private List<AbstractWidget> items;

  @XmlCDATA @XmlElement private String template;

  @Override
  public List<AbstractWidget> getItems() {
    return items;
  }

  @Override
  public Set<String> getExtraNames() {
    return Stream.of(getEventStart(), getEventStop(), getColorBy())
        .filter(StringUtils::notBlank)
        .collect(Collectors.toSet());
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getColorBy() {
    return colorBy;
  }

  public void setColorBy(String colorBy) {
    this.colorBy = colorBy;
  }

  public String getOnChange() {
    return onChange;
  }

  public void setOnChange(String onChange) {
    this.onChange = onChange;
  }

  public String getOnDelete() {
    return onDelete;
  }

  public void setOnDelete(String onDelete) {
    this.onDelete = onDelete;
  }

  public String getEventStart() {
    return eventStart;
  }

  public void setEventStart(String eventStart) {
    this.eventStart = eventStart;
  }

  public String getEventStop() {
    return eventStop;
  }

  public void setEventStop(String eventStop) {
    this.eventStop = eventStop;
  }

  public Integer getEventLength() {
    return eventLength;
  }

  public void setEventLength(Integer eventLength) {
    this.eventLength = eventLength;
  }

  public Integer getDayLength() {
    return dayLength;
  }

  public void setDayLength(Integer dayLength) {
    this.dayLength = dayLength;
  }

  public void setItems(List<AbstractWidget> items) {
    this.items = items;
  }

  public Boolean getCanDelete() {
    return canDelete;
  }

  public void setCanDelete(Boolean canDelete) {
    this.canDelete = canDelete;
  }

  public Boolean getCanNew() {
    return canNew;
  }

  public void setCanNew(Boolean canNew) {
    this.canNew = canNew;
  }

  public List<CalendarEventHilite> getHilites() {
    return hilites;
  }

  public void setHilites(List<CalendarEventHilite> hilites) {
    this.hilites = hilites;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }
}
