package com.axelor.meta.schema.actions;

abstract class ActionIndex extends Action {

	private static final ThreadLocal<Integer> INDEX = new ThreadLocal<Integer>();
	
	public void setIndex(int index) {
		INDEX.set(index);
	}

	public int getIndex() {
		final Integer n = INDEX.get();
		if (n == null) {
			return 0;
		}
		INDEX.remove();
		return n;
	}
}
