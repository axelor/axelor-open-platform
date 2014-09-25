/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
package com.axelor.script;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.axelor.rpc.Context;
import com.axelor.script.GroovyScriptHelper;
import com.axelor.test.db.Contact;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestScripts extends ScriptTest {

	private int counter = 0;

    @Test
    public void test01_casts() {

    	GroovyScriptHelper helper = new GroovyScriptHelper(context());

    	Object actual;

    	actual = helper.eval("__parent__");
    	Assert.assertTrue(actual instanceof Context);

    	actual = helper.eval("__ref__");
    	Assert.assertTrue(actual instanceof Contact);

    	actual = helper.eval("__parent__ as Contact");
    	Assert.assertTrue(actual instanceof Contact);

    	actual = helper.eval("(__ref__ as Contact).fullName");
    	Assert.assertTrue(actual instanceof String);
    	Assert.assertEquals("John Smith", actual);

    	actual = helper.eval("(__ref__ as Contact).fullName + ' (" + counter + ")'");
    }

    //@Test
    public void test02_permgen() {
    	while (counter++ < 5000) {
    		test01_casts();
    	}
    }
}
