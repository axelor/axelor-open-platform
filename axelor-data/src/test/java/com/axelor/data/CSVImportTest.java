package com.axelor.data;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
		
		importer.addListener(new Listener() {
			@Override
			public void imported(Model bean) {
				log.info("Bean saved : {}(id={})",
						bean.getClass().getSimpleName(),
						bean.getId());
				records.add(bean);
			}

			@Override
			public void imported(Integer counter) {
				log.info("Record (count): " + counter);
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
