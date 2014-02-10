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
package com.axelor.meta.schema.actions;

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