/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
package com.axelor.wkf.db.node;

import javax.persistence.Entity;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.db.annotations.Widget;
import com.axelor.wkf.db.Node;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@Entity
public class TimerEvent extends Node {

	@Widget(title = /*$$(*/"Time duration"/*)*/)
	private LocalTime timeDuration;

	@Widget(title = /*$$(*/"Time date"/*)*/)
	private LocalDate timeDate;

	@Widget(title = /*$$(*/"Time cycle"/*)*/)
	private DateTime timeCycle;

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

	public DateTime getTimeCycle() {
		return timeCycle;
	}

	public void setTimeCycle(DateTime timeCycle) {
		this.timeCycle = timeCycle;
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
		tsh.add("timeCycle", this.getTimeCycle());

		return tsh.omitNullValues().toString();
	}

	/**
	 * Find a <code>IntermediateTimerEvent</code> by <code>id</code>.
	 *
	 */
	public static TimerEvent find(Long id) {
		return JPA.find(TimerEvent.class, id);
	}

	/**
	 * Return a {@link Query} instance for <code>IntermediateTimerEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<? extends TimerEvent> all() {
		return JPA.all(TimerEvent.class);
	}

	/**
	 * A shortcut method to <code>IntermediateTimerEvent.all().filter(...)</code>
	 *
	 */
	public static Query<? extends TimerEvent> filter(String filter, Object... params) {
		return all().filter(filter, params);
	}

}
