/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.axelor.test.GuiceExtension;
import com.axelor.test.GuiceModules;
import com.google.inject.AbstractModule;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GuiceExtension.class)
@GuiceModules({EventModule.class, TestModule.class, TestEventsRawTypeHierarchy.RawTypeModule.class})
public class TestEventsRawTypeHierarchy {

  private static final List<String> result = new ArrayList<>();

  // Plain (non-parameterized) event class hierarchy.
  public static class ModelEvent {}

  public static class ContactEvent extends ModelEvent {}

  // onModel observes the supertype: per CDI spec it must fire for any subtype event.
  // onContact observes the exact type: must fire only for ContactEvent.
  public static class EventHandler {
    public void onModel(@Observes ModelEvent e) {
      result.add("model");
    }

    public void onContact(@Observes ContactEvent e) {
      result.add("contact");
    }
  }

  public static class RawTypeModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(EventHandler.class);
    }
  }

  @Inject private Event<ContactEvent> contactEvent;
  @Inject private Event<ModelEvent> modelEvent;

  @BeforeEach
  void clearResult() {
    result.clear();
  }

  @Test
  public void testSupertypeObserverFiresForSubtypeEvent() {
    // Firing a ContactEvent must invoke both onContact (exact match) and
    // onModel (supertype match).
    contactEvent.fire(new ContactEvent());
    List<String> sorted = new ArrayList<>(result);
    Collections.sort(sorted);
    assertEquals(List.of("contact", "model"), sorted);
  }

  @Test
  public void testExactTypeObserverDoesNotFireForSupertypeEvent() {
    // Firing a plain ModelEvent must invoke only onModel, not onContact.
    modelEvent.fire(new ModelEvent());
    assertEquals(List.of("model"), result);
  }
}
