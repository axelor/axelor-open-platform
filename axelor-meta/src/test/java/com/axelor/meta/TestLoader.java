package com.axelor.meta;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;

public class TestLoader extends AbstractTest {
	
	private MetaLoader loader;

	@Before
	public void setUp() {
		loader = new MetaLoader();
	}
	
	@Test
	public void testLoad() {
		loader.load("/tmp");
	}
	
	@Test
	public void testValidate() {
		String xml = "<form name=\"some-name\" title=\"Some Name\">" +
				"<field name=\"some\"/>" +
				"<group title=\"Group\" colSpan=\"4\" cols=\"3\" colWidths=\"33%,33%,33%\">" +
				"<button name=\"button1\" title=\"Click 1\"/>" +
				"<button name=\"button2\" title=\"Click 2\"/>" +
				"</group>" +
				"<field name=\"other\"/>" +
				"</form>";
		
		try {
			loader.fromXML(xml);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
}
