/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.rpc;

import java.sql.BatchUpdateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

@JsonInclude(Include.NON_EMPTY)
public class Response {

	public static int STATUS_FAILURE = -1;
	public static int STATUS_LOGIN_INCORRECT = -5;
	public static int STATUS_LOGIN_REQUIRED = -7;
	public static int STATUS_LOGIN_SUCCESS = -8;
	public static int STATUS_MAX_LOGIN_ATTEMPTS_EXCEEDED = -6;
	public static int STATUS_SERVER_TIMEOUT = -100;
	public static int STATUS_SUCCESS = 0;
	public static int STATUS_TRANSPORT_ERROR = -90;
	public static int STATUS_VALIDATION_ERROR = -4;

	private int status;
	
	@JsonInclude(Include.NON_DEFAULT)
	private int offset = -1;
	
	@JsonInclude(Include.NON_DEFAULT)
	private long total = -1;

	private Object data;
	
	private Map<String, String> errors;

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public void setOffset(int offset) {
		this.offset = offset;
	}
	
	public long getTotal() {
		return total;
	}
	
	public void setTotal(long count) {
		this.total = count;
	}
	
	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
	
	public Map<String, String> getErrors() {
		return errors;
	}

	public void setErrors(Map<String, String> errors) {
		this.errors = errors;
		this.setStatus(STATUS_VALIDATION_ERROR);
	}

	public void addError(String fieldName, String errorMessage) {
		if (this.errors == null) {
			this.errors = new HashMap<String, String>();
		}
		this.errors.put(fieldName, errorMessage);
		this.setStatus(STATUS_VALIDATION_ERROR);
	}
	
	public void setException(Throwable throwable) {
		
		Throwable cause = Throwables.getRootCause(throwable);
		if (cause instanceof BatchUpdateException) {
			cause = ((BatchUpdateException) cause).getNextException();
		}
		
		Map<String, Object> report = Maps.newHashMap();
		
		report.put("class", throwable.getClass());
		report.put("message", cause.getMessage());
		report.put("string", cause.toString());
		report.put("stacktrace", Throwables.getStackTraceAsString(throwable));
		report.put("cause", Throwables.getStackTraceAsString(cause));
		
		this.setData(report);
		this.setStatus(STATUS_FAILURE);
	}

	public Object getItem(int index) {
		try {
			return ((List<?>) data).get(index);
		} catch(Exception e){
		}
		return null;
	}
}
