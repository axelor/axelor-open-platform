package com.axelor.rpc;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Request {

	public static final String TEXT_MATCH_EXACT = "exact";
	public static final String TEXT_MATCH_SUBSTRING = "substring";
	public static final String TEXT_MATCH_STARTSWITH = "startsWith";

	private String textMatchStyle;

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

	public String getTextMatchStyle() {
		return textMatchStyle;
	}

	public void setTextMatchStyle(String textMatchStyle) {
		this.textMatchStyle = textMatchStyle;
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
