package com.axelor.meta.views;

import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.meta.ActionHandler;

@XmlType
public class ActionMethod extends Action {
	
	@XmlType
	public static class Call extends Element {
		
		@XmlAttribute
		private String method;
		
		@XmlAttribute(name = "class")
		private String controller;
		
		public String getMethod() {
			return method;
		}
		
		public void setMethod(String method) {
			this.method = method;
		}
		
		public String getController() {
			return controller;
		}
		
		public void setController(String controller) {
			this.controller = controller;
		}
	}
	
	@XmlElement(name = "call")
	private Call call;
	
	public Call getCall() {
		return call;
	}
	
	public void setCall(Call call) {
		this.call = call;
	}

	private boolean isRpc(String methodCall) {
		return Pattern.matches("(\\w+)\\((.*?)\\)", methodCall);
	}
	
	@Override
	public Object evaluate(ActionHandler handler) {
		if (isRpc(call.getMethod()))
			return handler.rpc(call.getController(), call.getMethod());
		return handler.call(call.getController(), call.getMethod());
	}
	
	@Override
	public Object wrap(ActionHandler handler) {
		return evaluate(handler);
	}
}