/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.wkf.db.node;

import javax.persistence.Entity;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.Node;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@Entity
public class IntermediateTimerEvent extends Node {
	
	private LocalTime timeDuration;
	
	private LocalDate timeDate;
	
	private DateTime timeCyle;

	
	public LocalTime getTimeDuration() {
		return timeDuration;
	}

	public void setTimeDuration(LocalTime timeDuration) {
		this.timeDuration = timeDuration;
	}

	public LocalDate getTimeDate() {
		return timeDate;
	}

	public void setTimeDate(LocalDate timeDate) {
		this.timeDate = timeDate;
	}

	public DateTime getTimeCyle() {
		return timeCyle;
	}

	public void setTimeCyle(DateTime timeCyle) {
		this.timeCyle = timeCyle;
	}

	@Override
	public String toString() {
		ToStringHelper tsh = Objects.toStringHelper(this);

		tsh.add("id", this.getId());
		tsh.add("name", this.getName());
		tsh.add("type", this.getType());
		tsh.add("ref", this.getRef());
		tsh.add("timeDuration", this.getTimeDuration());
		tsh.add("timeDate", this.getTimeDate());
		tsh.add("timeCyle", this.getTimeCyle());

		return tsh.omitNullValues().toString();
	}
	
	/**
	 * Find a <code>IntermediateTimerEvent</code> by <code>id</code>.
	 *
	 */
	public static IntermediateTimerEvent find(Long id) {
		return JPA.find(IntermediateTimerEvent.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>IntermediateTimerEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<IntermediateTimerEvent> allIntermediateTimerEvent() {
		return JPA.all(IntermediateTimerEvent.class);
	}
	
	/**
	 * A shortcut method to <code>IntermediateTimerEvent.all().filter(...)</code>
	 *
	 */
	public static Query<IntermediateTimerEvent> filterIntermediateTimerEvent(String filter, Object... params) {
		return JPA.all(IntermediateTimerEvent.class).filter(filter, params);
	}
	
}
