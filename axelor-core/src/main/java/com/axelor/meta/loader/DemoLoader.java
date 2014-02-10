package com.axelor.meta.loader;

class DemoLoader extends DataLoader {

	private static final String DATA_DIR_NAME = "data-demo";

	@Override
	protected String getDirName() {
		return DATA_DIR_NAME;
	}
}
