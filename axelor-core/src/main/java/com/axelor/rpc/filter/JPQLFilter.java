package com.axelor.rpc.filter;

import java.util.Arrays;
import java.util.List;

public class JPQLFilter extends Filter {

	private String jpql;

	private Object[] params;

	public JPQLFilter(String jpql, Object... params) {
		this.jpql = jpql;
		this.params = params;
	}

	@Override
	public String getQuery() {
		return "(" + this.jpql + ")";
	}

	@Override
	public List<Object> getParams() {
		return Arrays.asList(this.params);
	}
}
