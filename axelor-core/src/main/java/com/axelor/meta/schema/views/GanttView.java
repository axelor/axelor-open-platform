/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
package com.axelor.meta.schema.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("gantt")
public class GanttView extends AbstractView {

	@XmlAttribute
	private String taskStart;

	@XmlAttribute
	private String taskDuration;

	@XmlAttribute
	private String taskParent;

	@XmlAttribute
	private String taskSequence;

	@XmlAttribute
	private String taskProgress;

	@XmlAttribute(name = "x-start-to-start")
	private String startToStart;

	@XmlAttribute(name = "x-start-to-finish")
	private String startToFinish;

	@XmlAttribute(name = "x-finish-to-start")
	private String finishToStart;

	@XmlAttribute(name = "x-finish-to-finish")
	private String finishToFinish;

	@XmlElement(name="field", type=Field.class)
	private List<AbstractWidget> items;

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

	public List<AbstractWidget> getItems() {
		return items;
	}

	public void setItems(List<AbstractWidget> items) {
		this.items = items;
	}
}
