/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
import java.net.URL;
import java.util.List;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;
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
    long count = Beans.get(ViewGenerator.class).process(List.of("base-grid"), true);
    assertEquals(1, count);

    MetaView originalView = viewRepository.findByNameAndComputed("base-grid", false);
    MetaView metaView = viewRepository.findByNameAndComputed("base-grid", true);
    GridView view = (GridView) XMLViews.unmarshal(metaView.getXml()).getViews().get(0);

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
  }

  Field findFieldInView(GridView view, String field) {
    return view.getItems().stream()
        .filter(it -> it instanceof Field && ((Field) it).getName().equals(field))
        .map(Field.class::cast)
        .findFirst()
        .orElse(null);
  }

  Button findButtonInView(GridView view, String button) {
    return view.getItems().stream()
        .filter(it -> it instanceof Button && ((Button) it).getName().equals(button))
        .map(Button.class::cast)
        .findFirst()
        .orElse(null);
  }
}
