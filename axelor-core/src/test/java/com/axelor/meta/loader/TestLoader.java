/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
package com.axelor.meta.loader;

import javax.xml.bind.JAXBException;

import org.junit.Test;

import com.axelor.meta.MetaTest;
import com.axelor.meta.loader.XMLViews;

public class TestLoader extends MetaTest {
	
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
			XMLViews.fromXML(xml);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
}
