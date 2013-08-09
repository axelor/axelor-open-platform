package com.axelor.meta.script;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.axelor.meta.db.Contact;
import com.axelor.rpc.Context;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestScripts extends BaseTest {

    @Test
    public void test02_casts() {

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
    }
}
