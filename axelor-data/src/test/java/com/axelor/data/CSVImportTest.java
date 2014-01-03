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
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.data.csv.CSVImporter;
import com.axelor.db.Model;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Injector;

@RunWith(GuiceRunner.class)
@GuiceModules(MyModule.class)
public class CSVImportTest {
	
	protected final transient Logger log = LoggerFactory.getLogger(CSVImportTest.class);
	
	@Inject
	Injector injector;
	
	@Test
	public void test() throws ClassNotFoundException {
		final List<Model> records = Lists.newArrayList();
		CSVImporter importer = new CSVImporter(injector, "data/csv-multi-config.xml");
		
		Map<String, Object> context = Maps.newHashMap();
		
		context.put("CUSTOMER_PHONE", "+3326253225");
		
		importer.setContext(context);
		
		importer.addListener(new Listener() {
			@Override
			public void imported(Model bean) {
				log.info("Bean saved : {}(id={})",
						bean.getClass().getSimpleName(),
						bean.getId());
				records.add(bean);
			}

			@Override
			public void imported(Integer total, Integer success) {
				log.info("Record (total): " + total);
				log.info("Record (success): " + success);
			}

			@Override
			public void handle(Model bean, Exception e) {
				
			}
		});
		
		importer.runTask(new ImportTask(){
						
			@Override
			public void configure() throws IOException {
				input("[sale.order]", new File("data/csv-multi/so1.csv"));
				input("[sale.order]", new File("data/csv-multi/so2.csv"));
			}
			
			@Override
			public boolean handle(ImportException exception) {
				log.error("Import error : " + exception);
				return false;
			}
			
			@Override
			public boolean handle(IOException e) {
				log.error("IOException error : " + e);
				return true;
			}
			
		});
		
		
	}

}
