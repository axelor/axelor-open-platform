/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
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
