package com.axelor.meta.web;

import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class Hello {
	
	public void say(ActionRequest request, ActionResponse response) {
		response.setFlash("Hello World!!!");
	}
	
	public String say(String what) {
		return "Say: " + what;
	}
}
