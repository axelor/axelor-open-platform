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
package com.axelor.wkf.workflow;

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
	
	private Logger log = LoggerFactory.getLogger(getClass());
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
