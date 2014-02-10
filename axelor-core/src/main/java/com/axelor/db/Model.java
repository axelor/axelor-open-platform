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
