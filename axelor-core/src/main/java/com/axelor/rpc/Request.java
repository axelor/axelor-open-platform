/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.rpc;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Request {

	private int limit;

	private int offset;

	private List<String> sortBy;

	private Map<String, Object> data;

	private List<Object> records;

	private Criteria criteria;
	
	private List<String> fields;

	private Map<String, List<String>> related;

	private String model;
	
	public String getModel() {
		return model;
	}

	/**
	 * Set the model class that represents the request {@link #data}.
	 * 
	 * @param model the model class
	 */
	public void setModel(String model) {
		this.model = model;
	}
	
	/**
	 * Get the entity class on which the operation is being performed.
	 * 
	 * @return bean class
	 */
	@JsonIgnore
	public Class<?> getBeanClass() {
		try {
			return Class.forName(model);
		} catch (NullPointerException e) {
		} catch (ClassNotFoundException e) {
		}
		return null;
	}

	public int getLimit() {
		return limit;
	}
	
	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public void setOffset(int offset) {
		this.offset = offset;
	}
	
	public List<String> getSortBy() {
		return sortBy;
	}
	
	public void setSortBy(List<String> sortBy) {
		this.sortBy = sortBy;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}
	
	public List<Object> getRecords() {
		return records;
	}
	
	public void setRecords(List<Object> records) {
		this.records = records;
	}
	
	public List<String> getFields() {
		return fields;
	}
	
	public void setFields(List<String> fields) {
		this.fields = fields;
	}

	public Map<String, List<String>> getRelated() {
		return related;
	}

	public void setRelated(Map<String, List<String>> related) {
		this.related = related;
	}

	@JsonIgnore
	public Criteria getCriteria() {

		if (criteria != null) {
			return criteria;
		}
		
		try {
			return criteria = Criteria.parse(this);
		} catch (Exception ex) {
		}

		return null;
	}
}
