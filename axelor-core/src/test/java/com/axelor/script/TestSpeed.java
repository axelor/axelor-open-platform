/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

import org.junit.Test;

import com.axelor.rpc.Context;
import com.axelor.test.db.Contact;

@SuppressWarnings("all")
public class TestSpeed extends ScriptTest {

    private static final int COUNT = 1000;

    private static final String EXPR_INTERPOLATION =
    		"\"(${title.name}) = $firstName $lastName ($fullName) = ($__user__)\"";

    private static final String EXPR_CONCAT =
    		"'(' + title.name + ') = ' + firstName + ' ' + lastName + ' (' + fullName + ') = (' + __user__ + ')' ";

    private static final String EXPR_ELVIS =
    		"'(' + __this__?.title?.name + ') = ' + __this__?.firstName + ' ' + __this__?.lastName + ' (' + __this__?.fullName + ') = (' + __user__ + ')' ";

    // false all, to evaluate all conditions
    private static final String EXPR_CONDITION =
    		"(title instanceof Contact || fullName == 'foo') || (__ref__ instanceof Title) || (__parent__ == 0.102) || (__self__ == __this__)";

    private void doTest(String expr) {
    	GroovyScriptHelper helper = new GroovyScriptHelper(context());
        for(int i = 0 ; i < COUNT ; i ++) {
        	Object result = helper.eval(expr);
        }
    }

    @Test
    public void test10_warmup() {
    	doTest(EXPR_INTERPOLATION);
    }

    @Test
    public void test20_interpolation() {
    	doTest(EXPR_INTERPOLATION);
    }

    @Test
    public void test21_concat() {
    	doTest(EXPR_CONCAT);
    }

    @Test
    public void test22_elvis() {
    	doTest(EXPR_ELVIS);
    }

    @Test
    public void test23_condition() {
    	doTest(EXPR_CONDITION);
    }

	@Test
	public void test40_java_concat() {
		Context context = context();
		Contact contact = context.asType(Contact.class);
		for (int i = 0; i < COUNT; i++) {
			String x =  "(" + contact.getTitle().getName() + ") ="
							+ " " + contact.getFirstName()
							+ " " + contact.getLastName()
							+ " (" + contact.getFullName() + ") ="
							+ " (" + context.get("__user__") + ")";

		}
	}
}
