package com.axelor.rpc;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
		if (context != null)
			return context;
		if (getData() == null)
			return null;
		return context = Context.create((Map) getData().get("context"), getBeanClass());
	}
}
