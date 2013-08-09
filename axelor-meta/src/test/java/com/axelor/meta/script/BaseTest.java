package com.axelor.meta.script;

import java.util.Map;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.axelor.db.JPA;
import com.axelor.meta.AbstractTest;
import com.axelor.meta.db.Contact;
import com.axelor.rpc.Context;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BaseTest extends AbstractTest {

	@Before
    @Transactional
    public void prepare() {
    	if (Contact.all().count() > 0) return;
    	Contact c = new Contact();
    	c.setFirstName("John");
    	c.setLastName("Smith");
    	JPA.save(c);
    }

    protected Context context() {
        final Map<String, Object> data = Maps.newHashMap();
        data.put("_model", Contact.class.getName());
        data.put("firstName", "My");
        data.put("lastName", "Name");
        data.put("fullName", "Mr. My Name");
        data.put("title", ImmutableMap.of("name", "Mr.", "code", "mr"));

        Map<String, Object> ref = Maps.newHashMap();
        ref.put("_model", Contact.class.getName());
        ref.put("id", 1);

        data.put("_ref", ref);
        data.put("_parent", ref);

        return Context.create(data, Contact.class);
    }
}
