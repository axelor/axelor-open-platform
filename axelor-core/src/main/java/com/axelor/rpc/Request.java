/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
