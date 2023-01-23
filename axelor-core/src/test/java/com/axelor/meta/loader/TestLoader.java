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
            + "<panel>"
            + "  <field name=\"some\"/>"
            + "  <panel title=\"Group\" colSpan=\"4\">"
            + "    <button name=\"button1\" title=\"Click 1\" onClick=\"action.button1\"/>"
            + "    <button name=\"button2\" title=\"Click 2\" onClick=\"action.button2\"/>"
            + "  </panel>"
            + "  <field name=\"other\"/>"
            + "</panel>"
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
