package com.axelor.tool;

import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.axelor.tool.x2j.Generator;

@RunWith(GuiceRunner.class)
@GuiceModules({ TestModule.class })
public class GeneratorTest {

	InputStream read(String resource) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
	}
	
	@Test
	public void test() {
		Generator gen = new Generator("src/test/resources/domains", "target/src-gen");
		gen.clean();
		gen.start();
	}
}
