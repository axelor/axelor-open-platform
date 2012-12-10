package com.axelor.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.data.xml.XMLImporter;
import com.axelor.data.xml.XMLImporter.ImportTask;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.google.common.collect.Maps;
import com.google.inject.Injector;

@RunWith(GuiceRunner.class)
@GuiceModules(MyModule.class)
public class XMLImportTest {
	
	@Inject
	Injector injector;
	
	@Test
	public void test() throws FileNotFoundException {
		XMLImporter importer = new XMLImporter(injector, "data/xml-config.xml");
		Map<String, Object> context = Maps.newHashMap();
		
		context.put("LOCATION", "FR");
		context.put("DATE_FORMAT", "dd/MM/yyyy");
		
		importer.setContext(context);
		
		importer.runTask(new ImportTask(){
			
			@Override
			protected void configure() throws IOException {
				input("contacts.xml", new File("data/xml/contacts.xml"));
				input("contacts.xml", new File("data/xml/contacts-non-unicode.xml"), Charset.forName("ISO-8859-15"));
			}
			
			@Override
			protected boolean handle(ImportException exception) {
				System.err.println("Import error: " + exception);
				return true;
			}
		});
	}
}
