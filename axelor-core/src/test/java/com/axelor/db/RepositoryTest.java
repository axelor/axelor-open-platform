/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.axelor.JpaTest;
import com.axelor.JpaTestModule;
import com.axelor.inject.Beans;
import com.axelor.script.GroovyScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import com.axelor.test.db.repo.ContactRepositoryEx;
import java.util.HashMap;
import javax.inject.Inject;
import org.junit.Test;

@GuiceModules(RepositoryTest.MyModule.class)
public class RepositoryTest extends JpaTest {

  public static class MyModule extends JpaTestModule {

    @Override
    protected void configure() {
      bind(ContactRepository.class).to(ContactRepositoryEx.class);
      super.configure();
    }
  }

  @Inject private ContactRepository contacts;

  @Test
  public void test() {
    assertNotNull(contacts);
    assertTrue(contacts instanceof ContactRepositoryEx);

    Query<Contact> q = contacts.all();
    assertNotNull(q);

    // test manual instantiation
    ContactRepository repo = Beans.get(ContactRepository.class);
    assertNotNull(repo);
    assertTrue(repo instanceof ContactRepositoryEx);

    // test manual instantiation by model class name
    JpaRepository<Contact> repo2 = JpaRepository.of(Contact.class);
    assertNotNull(repo2);
    assertTrue(repo2 instanceof ContactRepositoryEx);

    // test groovy expression
    ScriptBindings bindings = new ScriptBindings(new HashMap<String, Object>());
    GroovyScriptHelper helper = new GroovyScriptHelper(bindings);
    String expr = "__repo__(Contact)";
    Object obj = helper.eval(expr);
    assertNotNull(obj);
    assertTrue(obj instanceof ContactRepositoryEx);
  }
}
