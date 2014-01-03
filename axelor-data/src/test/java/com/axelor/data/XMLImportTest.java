/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
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
			public void configure() throws IOException {
				input("contacts.xml", new File("data/xml/contacts.xml"));
				input("contacts.xml", new File("data/xml/contacts-non-unicode.xml"), Charset.forName("ISO-8859-15"));
			}
			
			@Override
			public boolean handle(ImportException exception) {
				System.err.println("Import error: " + exception);
				return true;
			}
		});
	}
}
