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
