/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.wkf.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Scanner;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.data.ImportException;
import com.axelor.data.ImportTask;
import com.axelor.data.xml.XMLImporter;
import com.google.inject.Injector;

public class WorkflowImporter {

	private static final String CONFIG = "data/xml-bpmn-config.xml";
	
	private Logger log = LoggerFactory.getLogger( getClass() );
	private XMLImporter xmlImporter;
	private File configFile;
	
	@Inject
	public WorkflowImporter ( Injector injector ) throws IOException { 
		
		configFile = File.createTempFile("bpmn_config", ".tmp");
		configFile.deleteOnExit();
		
		FileWriter fileWriter = new FileWriter( configFile );
		BufferedWriter writer = new BufferedWriter( fileWriter );
		writer.write( convertStreamToString( Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG) ));
		writer.flush(); writer.close(); fileWriter.close();
		
		this.xmlImporter = new XMLImporter(injector, configFile.getAbsolutePath());
		
	}
	
	public void run( final String bpmnXml ){
		
		log.debug("BPMN to parse : {}", bpmnXml);
		
		xmlImporter.runTask(new ImportTask() {
			
			@Override
			public void configure() throws IOException {
				Reader reader = new StringReader( bpmnXml );
				input("[wkf.import]", reader);
			}
			
			@Override
			public boolean handle(ImportException exception) {
				exception.printStackTrace();
                return true;
            }
			
		});
		
	}
	
	@SuppressWarnings("resource")
	public static String convertStreamToString( InputStream is ) {
	    Scanner s = new Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
	
}
