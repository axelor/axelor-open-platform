/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.db.EntityHelper;
import com.axelor.rpc.Context;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestGroovy extends ScriptTest {

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

  @Test
  public void doJpaTest() {
    final ScriptHelper helper = new GroovyScriptHelper(context());
    final Object bean = helper.eval("doInJPA({ em -> em.find(Contact, id) })");
    assertNotNull(bean);
    assertTrue(bean instanceof Contact);
    assertEquals(contact.getId(), ((Contact) bean).getId());
    assertFalse(EntityHelper.isUninitialized((Contact) bean));
  }

  @Test
  public void testImport() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object actual;

    // Not need of FQN
    actual = helper.eval("LocalDate.of(2020, 5, 22)");
    assertEquals(LocalDate.of(2020, 5, 22), actual);

    // __repo__
    actual = helper.eval("__repo__(Contact)");
    assertTrue(actual instanceof ContactRepository);
  }

  @Test
  public void testEvalCast() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object actual;

    actual = helper.eval("__parent__");
    assertTrue(actual instanceof Context);

    actual = helper.eval("__ref__");
    assertTrue(actual instanceof Contact);

    actual = helper.eval("__parent__ as Contact");
    assertTrue(actual instanceof Contact);

    actual = helper.eval("(__ref__ as Contact).fullName");
    assertTrue(actual instanceof String);

    actual = helper.eval("(__ref__ as Contact).fullName + ' (" + 0 + ")'");
    assertEquals("Mr. John Smith (0)", actual);
  }

  @Test
  public void testInterpolation() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object result = helper.eval(EXPR_INTERPOLATION);

    assertEquals("(Mrs.) = John NAME (Mrs. John NAME) = (null)", result.toString());
  }

  @Test
  public void testConcat() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object result = helper.eval(EXPR_CONCAT);

    assertEquals("(Mrs.) = John NAME (Mrs. John NAME) = (null)", result);
  }

  @Test
  public void testElvis() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object result = helper.eval(EXPR_ELVIS);

    assertEquals("(Mrs.) = John NAME (Mrs. John NAME) = (null)", result);
  }

  @Test
  public void testCondition() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object result = helper.eval(EXPR_CONDITION);

    assertTrue((Boolean) result);
  }
}
