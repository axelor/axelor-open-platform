package com.axelor.data;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.data.xml.XMLImporter;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.inject.Injector;

@RunWith(GuiceRunner.class)
@GuiceModules(MyModule.class)
public class XMLImportTest {
	
	@Inject
	Injector injector;
	
	@Test
	public void test() throws FileNotFoundException, ImportException {
		XMLImporter importer = new XMLImporter(injector, "data/xml-config.xml");
		Multimap<String, Reader> inputs = HashMultimap.create();
		Map<String, Object> context = Maps.newHashMap();
		
		inputs.put("contacts.xml", new FileReader("data/xml/contacts.xml"));
		
		context.put("LOCATION", "FR");
		context.put("DATE_FORMAT", "dd/MM/yyyy");

		importer.setContext(context);
		
		importer.process(inputs);
	}
}
