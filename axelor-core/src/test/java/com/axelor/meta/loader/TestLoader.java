/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.meta.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.axelor.AbstractTest;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.test.db.Contact;
import java.util.List;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;

public class TestLoader extends AbstractTest {

  @Inject private ViewLoader loader;

  @Test
  public void testValidate() {
    String xml =
        "<form name=\"some-name\" title=\"Some Name\" model=\"com.axelor.test.db.Contact\">"
            + "<field name=\"some\"/>"
            + "<group title=\"Group\" colSpan=\"4\" cols=\"3\" colWidths=\"33%,33%,33%\">"
            + "<button name=\"button1\" title=\"Click 1\" onClick=\"action.button1\"/>"
            + "<button name=\"button2\" title=\"Click 2\" onClick=\"action.button2\"/>"
            + "</group>"
            + "<field name=\"other\"/>"
            + "</form>";
    try {
      XMLViews.fromXML(xml);
    } catch (JAXBException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testDefault() {
    List<AbstractView> views = loader.createDefaults(Contact.class);
    assertNotNull(views);
    assertEquals(2, views.size());

    for (AbstractView view : views) {
      String text = XMLViews.toXml(view, true);
      assertNotNull(text);
    }
  }
}
