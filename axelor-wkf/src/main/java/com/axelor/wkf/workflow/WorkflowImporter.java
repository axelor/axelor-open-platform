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
		BufferedWriter writer = new BufferedWriter( new FileWriter( configFile ) );
		writer.write( convertStreamToString( Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG) ));
		writer.flush(); writer.close();
		
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
		
		configFile.deleteOnExit();
		
	}
	
	public static String convertStreamToString( InputStream is ) {
	    Scanner s = new Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
	
}
