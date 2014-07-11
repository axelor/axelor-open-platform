package com.axelor.tools.x2j;

public interface GeneratorListener {

	void onGenerate(String name, String path, int cardinality);
}
