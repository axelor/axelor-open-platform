package com.axelor.data;


import java.io.IOException;

import org.junit.Test;

import com.google.inject.AbstractModule;

public class CSVDataTest {
	
	static class MyLauncher extends Launcher {
		
		@Override
		protected AbstractModule createModule() {
			
			return new MyModule();
		}
	}

	@Test
	public void testDefault() throws IOException {
		MyLauncher launcher = new MyLauncher();
		launcher.run("-c", "data/csv-config.xml", "-d", "data/csv");
	}
	
	@Test
	public void testMulti() throws IOException {
		MyLauncher launcher = new MyLauncher();
		launcher.run("-c", "data/csv-multi-config.xml", "-d", "data/csv-multi", "-Dsale.order=so1.csv,so2.csv");
	}
	
	@Test
	public void testData() throws IOException {
		MyLauncher launcher = new MyLauncher();
		launcher.run("-c", "data/csv-config-types.xml", "-d", "data/csv");
	}
}
