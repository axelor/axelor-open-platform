/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.axelor.common.ResourceUtils;
import com.axelor.db.JPA;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaTest;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.axelor.meta.schema.views.Button;
import com.axelor.meta.schema.views.Field;
import com.axelor.meta.schema.views.GridView;
import com.axelor.meta.schema.views.Hilite;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestComputedViews extends MetaTest {

  @Inject private MetaViewRepository viewRepository;

  @BeforeAll
  static void setup() {
    final ViewLoader loader = Beans.get(ViewLoader.class);

    JPA.runInTransaction(
        () -> {
          final URL url = ResourceUtils.getResource("com/axelor/meta/extends/Base.xml");
          final URL urlExtends = ResourceUtils.getResource("com/axelor/meta/extends/Extends.xml");
          try {
            loader.process(url, new Module("foo-module"), false);
            loader.process(urlExtends, new Module("bar-module"), false);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void testComputedViews() throws JAXBException {
    long count = Beans.get(ViewGenerator.class).process(List.of("base-grid"));
    assertEquals(1, count);

    MetaView originalView = viewRepository.findByNameAndComputed("base-grid", false);
    MetaView metaView = viewRepository.findByNameAndComputed("base-grid", true);
    GridView view = (GridView) XMLViews.unmarshal(metaView.getXml()).getViews().getFirst();

    assertEquals("bar-module", metaView.getModule());
    assertEquals(originalView.getPriority() + 1, metaView.getPriority());

    // insert dateOfBirth after fullName
    assertNotNull(findFieldInView(view, "fullName"));
    assertNotNull(findFieldInView(view, "dateOfBirth"));

    // replace phone by proEmail
    assertNull(findFieldInView(view, "phone"));
    assertNotNull(findFieldInView(view, "proEmail"));

    // replace btnTest from 'Btn Test' to 'Awesome'
    assertEquals("Awesome", findButtonInView(view, "btnTest").getTitle());

    // replace grid orderBy from 'fullName' to 'email'
    assertEquals("email", view.getOrderBy());

    // Attribute on extend views aren't managed
    assertNull(view.getCss());

    // Hilite extend
    assertNotNull(findHiliteInView(view, "$contains(email, 'gmial.com')"));
    assertNotNull(findHiliteInView(view, "!phone"));
  }

  Field findFieldInView(GridView view, String field) {
    return view.getItems().stream()
        .filter(it -> it instanceof Field f && f.getName().equals(field))
        .map(Field.class::cast)
        .findFirst()
        .orElse(null);
  }

  Button findButtonInView(GridView view, String button) {
    return view.getItems().stream()
        .filter(it -> it instanceof Button b && b.getName().equals(button))
        .map(Button.class::cast)
        .findFirst()
        .orElse(null);
  }

  Hilite findHiliteInView(GridView view, String condition) {
    var found =
        view.getHilites().stream()
            .filter(it -> it.getCondition().equals(condition))
            .findFirst()
            .orElse(null);
    if (found == null) {
      found =
          view.getItems().stream()
              .filter(Field.class::isInstance)
              .map(Field.class::cast)
              .map(Field::getHilites)
              .filter(Objects::nonNull)
              .flatMap(List::stream)
              .filter(it -> it.getCondition().equals(condition))
              .findFirst()
              .orElse(null);
    }
    return found;
  }
}
