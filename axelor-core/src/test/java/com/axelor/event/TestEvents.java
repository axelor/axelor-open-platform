/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.axelor.db.Model;
import com.axelor.test.GuiceExtension;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Invoice;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GuiceExtension.class)
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

  public void onSaveInvoiceBeforeAndAfter(@Observes @After @Before SaveEvent<Invoice> event) {
    result.add("SaveEvent<Invoice> before after");
  }

  private Event<SaveEvent<Contact>> contactEvent;
  @Inject private Event<SaveEvent<Invoice>> invoiceEvent;

  @Inject private Event<SaveEvent<? extends Model>> modelEvent;
  @Inject private Event<SaveEvent<?>> anyEvent;

  @Inject
  public TestEvents(Event<SaveEvent<Contact>> contactEvent) {
    this.contactEvent = contactEvent;
  }

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

    invoiceEvent.select(new BeforeImpl(), new AfterImpl()).fire(new SaveEvent<>(invoice));
    assertResult(
        "SaveEvent<?>",
        "SaveEvent<? extends Model>",
        "SaveEvent<Invoice>",
        "SaveEvent<Invoice> after",
        "SaveEvent<Invoice> before after");
  }
}
