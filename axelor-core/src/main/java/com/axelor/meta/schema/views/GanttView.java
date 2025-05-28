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

import com.axelor.common.StringUtils;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@JsonTypeName("gantt")
public class GanttView extends AbstractView implements ContainerView {

  @XmlAttribute private String mode;

  @XmlAttribute private String taskStart;

  @XmlAttribute private String taskDuration;

  @XmlAttribute private String taskEnd;

  @XmlAttribute private String taskParent;

  @XmlAttribute private String taskSequence;

  @XmlAttribute private String taskProgress;

  @XmlAttribute private String taskUser;

  @XmlAttribute(name = "x-start-to-start")
  private String startToStart;

  @XmlAttribute(name = "x-start-to-finish")
  private String startToFinish;

  @XmlAttribute(name = "x-finish-to-start")
  private String finishToStart;

  @XmlAttribute(name = "x-finish-to-finish")
  private String finishToFinish;

  @XmlElement(name = "field", type = PanelField.class)
  private List<AbstractWidget> items;

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getTaskStart() {
    return taskStart;
  }

  public void setTaskStart(String taskStart) {
    this.taskStart = taskStart;
  }

  public String getTaskDuration() {
    return taskDuration;
  }

  public void setTaskDuration(String taskDuration) {
    this.taskDuration = taskDuration;
  }

  public String getTaskEnd() {
    return taskEnd;
  }

  public void setTaskEnd(String taskEnd) {
    this.taskEnd = taskEnd;
  }

  public String getTaskParent() {
    return taskParent;
  }

  public void setTaskParent(String taskParent) {
    this.taskParent = taskParent;
  }

  public String getTaskSequence() {
    return taskSequence;
  }

  public void setTaskSequence(String taskSequence) {
    this.taskSequence = taskSequence;
  }

  public String getTaskProgress() {
    return taskProgress;
  }

  public void setTaskProgress(String taskProgress) {
    this.taskProgress = taskProgress;
  }

  public String getTaskUser() {
    return taskUser;
  }

  public void setTaskUser(String taskUser) {
    this.taskUser = taskUser;
  }

  public String getStartToStart() {
    return startToStart;
  }

  public void setStartToStart(String startToStart) {
    this.startToStart = startToStart;
  }

  public String getStartToFinish() {
    return startToFinish;
  }

  public void setStartToFinish(String startToFinish) {
    this.startToFinish = startToFinish;
  }

  public String getFinishToStart() {
    return finishToStart;
  }

  public void setFinishToStart(String finishToStart) {
    this.finishToStart = finishToStart;
  }

  public String getFinishToFinish() {
    return finishToFinish;
  }

  public void setFinishToFinish(String finishToFinish) {
    this.finishToFinish = finishToFinish;
  }

  @Override
  public List<AbstractWidget> getItems() {
    return items;
  }

  public void setItems(List<AbstractWidget> items) {
    this.items = items;
  }

  @Override
  public Set<String> getExtraNames() {
    return Stream.of(getTaskUser()).filter(StringUtils::notBlank).collect(Collectors.toSet());
  }
}
