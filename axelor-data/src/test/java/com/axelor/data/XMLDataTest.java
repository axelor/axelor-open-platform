package com.axelor.data;

import java.io.IOException;

import org.junit.Test;

import com.axelor.data.xml.XMLImporter;
import com.google.inject.AbstractModule;

public class XMLDataTest {

	static class Module extends MyModule {

		@Override
		protected void configureImport() {
			bind(Importer.class).to(XMLImporter.class);
		}
	}

	static class MyLauncher extends Launcher {
		
		@Override
		protected AbstractModule createModule() {
			
			return new Module();
		}
	}

	@Test
	public void testTypes() throws IOException {
		MyLauncher launcher = new MyLauncher();
		launcher.run("-c", "data/xml-config-types.xml", "-d", "data/xml");
	}
	
	@Test
	public void testDefault() throws IOException {
		MyLauncher launcher = new MyLauncher();
		launcher.run("-c", "data/xml-config.xml", "-d", "data/xml");
	}
}
