/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.test.db;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.axelor.db.EntityHelper;
import com.axelor.db.Model;

@Entity
@Table(name = "CONTACT_CIRCLE")
public class Circle extends Model {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CONTACT_CIRCLE_SEQ")
	@SequenceGenerator(name = "CONTACT_CIRCLE_SEQ", sequenceName = "CONTACT_CIRCLE_SEQ", allocationSize = 1)
	private Long id;
	
	@NotNull
	private String code;

	@NotNull
	private String name;

	public Circle() {
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}

	public Circle(String name, String title) {
		this.code = name;
		this.name = title;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String name) {
		this.code = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String title) {
		this.name = title;
	}
	
	@Override
	public String toString() {
		return EntityHelper.toString(this);
	}
}
