package com.axelor.data;

import java.io.IOException;

import org.junit.Test;

import com.axelor.data.Importer;
import com.axelor.data.Launcher;
import com.axelor.data.xml.XMLImporter;
import com.axelor.db.JpaModule;
import com.google.inject.AbstractModule;

public class XMLDataTest {

	public static class MyModule extends AbstractModule {
		
		@Override
		protected void configure() {
			install(new JpaModule("testUnit", true, true));
			bind(Importer.class).to(XMLImporter.class);
		}
	}

	static class MyLauncher extends Launcher {
		
		@Override
		protected AbstractModule createModule() {
			
			return new MyModule();
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
