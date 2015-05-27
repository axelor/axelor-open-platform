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
package com.axelor.db;

import javax.persistence.GeneratedValue;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;

/**
 * The base abstract model class to extend all domain objects.
 * 
 * The derived model classes should implement {@link #getId()} and
 * {@link #setId(Long)} using appropriate {@link GeneratedValue#strategy()}.
 * 
 * A generic implementation {@link JpaModel} should be used in most cases if
 * sequence of record ids are important.
 * 
 */
@MappedSuperclass
public abstract class Model {

	@Version
	private Integer version;
	
	// Represents the selected state of the record in the UI widgets
	@Transient
	private transient boolean selected;

	private Boolean archived;

	public abstract Long getId();

	public abstract void setId(Long id);
	
	public Boolean getArchived() {
		return archived;
	}
	
	public void setArchived(Boolean archived) {
		this.archived = archived;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}
	
	/**
	 * Set the selected state of the record. The UI widget will use this flag to
	 * mark/unmark the selection state.
	 * 
	 * @param selected
	 *            selected state flag
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * Check whether the record is selected in the UI widget.
	 * 
	 * @return selection state
	 */
	public boolean isSelected() {
		return selected;
	}
}
