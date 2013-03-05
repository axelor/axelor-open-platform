package com.axelor.meta.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.meta.ActionHandler;
import com.google.common.base.Objects;

@XmlType
public class ActionTest extends Action {
	
	@XmlElement(name = "test")
	private List<Test> tests;

	@Override
	public Object wrap(ActionHandler handler) {
		return evaluate(handler);
	}

	@Override
	public Object evaluate(ActionHandler handler) {

		for(Test condition : tests) {
			if (condition.test(handler)) {
				return condition.evaluate(handler);
			}
		}

		return false;
	}
	
	@XmlType
	public static class Test extends Act {
		
		@XmlAttribute(name = "expression")
		private String expr;
		
		public String getExpr() {
			return expr;
		}

		public boolean evaluate(ActionHandler handler) {
			return this.test(handler, getExpr());
		}
		
		@Override
		public String toString() {
			return Objects.toStringHelper(getClass())
					.add("expression", getExpr())
					.add("condition", getCondition())
					.toString();
		}
		
	}

}
