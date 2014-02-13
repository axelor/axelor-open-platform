/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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
package com.axelor.script;

import java.util.Map;

import org.junit.Before;

import com.axelor.AbstractTest;
import com.axelor.db.JPA;
import com.axelor.rpc.Context;
import com.axelor.test.db.Contact;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

public abstract class ScriptTest extends AbstractTest {

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
