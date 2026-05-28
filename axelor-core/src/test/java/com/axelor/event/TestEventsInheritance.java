/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.axelor.test.GuiceExtension;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.Contact;
import com.google.inject.AbstractModule;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GuiceExtension.class)
@GuiceModules({EventModule.class, TestModule.class, TestEventsInheritance.InheritanceModule.class})
public class TestEventsInheritance {

  private static final List<String> result = new ArrayList<>();

  // Base class with an observer method
  public static class BaseHandler {
    public void onSave(@Observes SaveEvent<?> event) {
      result.add("base");
    }
  }

  // Subclass - inherit BaseHandler.onSave.
  public static class ChildHandler extends BaseHandler {}

  // Subclass that overrides the observer.
  public static class OverridingChildHandler extends ChildHandler {
    @Override
    public void onSave(@Observes SaveEvent<?> event) {
      result.add("overriding");
    }
  }

  public static class InheritanceModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(ChildHandler.class);
      bind(OverridingChildHandler.class);
    }
  }

  @Inject private Event<SaveEvent<Contact>> event;

  @BeforeEach
  void clearResult() {
    result.clear();
  }

  @Test
  public void testObserverInheritance() {
    event.fire(new SaveEvent<>(new Contact()));

    List<String> sorted = new ArrayList<>(result);
    Collections.sort(sorted);

    // "base" → ChildHandler has no observer; fire through the inherited BaseHandler.onSave.
    // "overriding" → OverridingChildHandler fires through its own onSave override.
    assertEquals(List.of("base", "overriding"), sorted);
  }
}
