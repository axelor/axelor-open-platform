/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.event;

import static org.junit.Assert.assertEquals;

import com.axelor.db.Model;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Invoice;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GuiceRunner.class)
@GuiceModules({EventModule.class, TestModule.class})
public class TestEvents {

  private static List<String> result = new ArrayList<>();

  private static void assertResult(String... values) {

    String[] expecteds = Arrays.copyOf(values, values.length);
    String[] actuals = result.toArray(new String[] {});

    Arrays.sort(expecteds);
    Arrays.sort(actuals);

    String expected = String.join(",", expecteds);
    String actual = String.join(",", actuals);

    try {
      assertEquals(expected, actual);
    } finally {
      result.clear();
    }
  }

  public void onSaveAny(@Observes @Priority(0) SaveEvent<?> event) {
    result.add("SaveEvent<?>");
  }

  public void onSaveModel(@Observes @Priority(1) SaveEvent<? extends Model> event) {
    result.add("SaveEvent<? extends Model>");
  }

  public void onSaveContact(@Observes @Priority(2) SaveEvent<Contact> event) {
    result.add("SaveEvent<Contact>");
  }

  public void onSaveInvoice(@Observes SaveEvent<Invoice> event) {
    result.add("SaveEvent<Invoice>");
  }

  public void onSaveInvoiceAfter(@Observes @After SaveEvent<Invoice> event) {
    result.add("SaveEvent<Invoice> after");
  }

  @Inject private Event<SaveEvent<Contact>> contactEvent;
  @Inject private Event<SaveEvent<Invoice>> invoiceEvent;

  @Inject private Event<SaveEvent<? extends Model>> modelEvent;
  @Inject private Event<SaveEvent<?>> anyEvent;

  @Test
  public void test() {

    Contact contact = new Contact();
    Invoice invoice = new Invoice();

    contactEvent.fire(new SaveEvent<>(contact));
    assertResult("SaveEvent<?>", "SaveEvent<? extends Model>", "SaveEvent<Contact>");

    invoiceEvent.fire(new SaveEvent<>(invoice));
    assertResult("SaveEvent<?>", "SaveEvent<? extends Model>", "SaveEvent<Invoice>");

    modelEvent.fire(new SaveEvent<>(invoice));
    assertResult("SaveEvent<?>", "SaveEvent<? extends Model>");

    anyEvent.fire(new SaveEvent<>(invoice));
    assertResult("SaveEvent<?>");
  }

  @Test
  public void testQualifiers() {
    Invoice invoice = new Invoice();

    invoiceEvent.select(new AfterImpl()).fire(new SaveEvent<>(invoice));
    assertResult(
        "SaveEvent<?>",
        "SaveEvent<? extends Model>",
        "SaveEvent<Invoice>",
        "SaveEvent<Invoice> after");
  }
}
