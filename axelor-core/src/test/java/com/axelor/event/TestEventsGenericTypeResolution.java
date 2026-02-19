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
import com.google.inject.AbstractModule;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GuiceExtension.class)
@GuiceModules({
  EventModule.class,
  TestModule.class,
  TestEventsGenericTypeResolution.GenericModule.class
})
public class TestEventsGenericTypeResolution {

  private static final List<String> result = new ArrayList<>();

  // Generic base class
  public abstract static class GenericHandler<T extends Model> {
    public void onSave(@Observes SaveEvent<T> event) {
      result.add(event.getEntity().getClass().getSimpleName());
    }
  }

  // T resolved to Contact: observed type must become SaveEvent<Contact>.
  public static class ContactHandler extends GenericHandler<Contact> {}

  // T resolved to Invoice: observed type must become SaveEvent<Invoice>.
  public static class InvoiceHandler extends GenericHandler<Invoice> {}

  public static class GenericModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(ContactHandler.class);
      bind(InvoiceHandler.class);
    }
  }

  @Inject private Event<SaveEvent<Contact>> contactEvent;
  @Inject private Event<SaveEvent<Invoice>> invoiceEvent;

  @BeforeEach
  void clearResult() {
    result.clear();
  }

  @Test
  public void testGenericTypeResolution() {
    contactEvent.fire(new SaveEvent<>(new Contact()));
    assertEquals(List.of("Contact"), result);

    result.clear();

    invoiceEvent.fire(new SaveEvent<>(new Invoice()));
    assertEquals(List.of("Invoice"), result);
  }

  @Test
  public void testGenericTypeResolutionOrdering() {
    contactEvent.fire(new SaveEvent<>(new Contact()));
    invoiceEvent.fire(new SaveEvent<>(new Invoice()));

    List<String> sorted = new ArrayList<>(result);
    Collections.sort(sorted);
    assertEquals(List.of("Contact", "Invoice"), sorted);
  }
}
