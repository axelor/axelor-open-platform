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
package com.axelor.meta.script;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.axelor.rpc.Context;
import com.axelor.test.db.Contact;

@SuppressWarnings("all")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSpeed extends BaseTest {

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

    private void doTest(String expr, boolean dynamic) {
    	GroovyScriptHelper helper = new GroovyScriptHelper(context());
        for(int i = 0 ; i < COUNT ; i ++) {
        	Object result = dynamic ? helper.evalDynamic(expr) : helper.evalStatic(expr);
        }
    }

    @Test
    public void test10_warmup() {
    	doTest(EXPR_INTERPOLATION, true);
    	doTest(EXPR_INTERPOLATION, false);
    }

    @Test
    public void test20_interpolation_d() {
    	doTest(EXPR_INTERPOLATION, true);
    }

    @Test
    public void test20_interpolation_s() {
    	doTest(EXPR_INTERPOLATION, false);
    }

    @Test
    public void test21_concat_d() {
    	doTest(EXPR_CONCAT, true);
    }

    @Test
    public void test21_concat_s() {
    	doTest(EXPR_CONCAT, false);
    }

    @Test
    public void test22_elvis_d() {
    	doTest(EXPR_ELVIS, true);
    }

    @Test
    public void test22_elvis_s() {
    	doTest(EXPR_ELVIS, false);
    }

    @Test
    public void test23_condition_d() {
    	doTest(EXPR_CONDITION, true);
    }

    @Test
    public void test23_condition_s() {
    	doTest(EXPR_CONDITION, false);
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
