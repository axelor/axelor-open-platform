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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;

public class ActionRequest extends Request {

	private String action;
	
	private Context context;
	
	public String getAction() {
		return action;
	}
	
	public void setAction(String action) {
		this.action = action;
	}
	
	@JsonIgnore
	@SuppressWarnings("all")
	public Context getContext() {
		if (context != null) {
			return context;
		}
		if (getData() == null) {
			return null;
		}
		
		Map<String, Object> data = getData();
		Map<String, Object> ctx = Maps.newHashMap();
		Class<?> klass = getBeanClass();

		if (data.get("context") != null) {
			ctx.putAll((Map) data.get("context"));
		}
		if (data.get("_domainContext") != null) {
			ctx.putAll((Map) data.get("_domainContext"));
		}
		if (ctx.get("_model") != null) {
			try {
				klass = Class.forName((String) ctx.get("_model"));
			} catch (Exception e) {
			}
		}

		return context = Context.create(ctx, klass);
	}
}
