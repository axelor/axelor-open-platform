/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.axelor.AbstractTest;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.test.db.Contact;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TestLoader extends AbstractTest {

  @Inject private ViewLoader loader;

  @Test
  public void testValidate() {
    String xml =
        """
        <form name="some-name" title="Some Name" model="com.axelor.test.db.Contact">\
        <panel>\
          <field name="some"/>\
          <panel title="Group" colSpan="4">\
            <button name="button1" title="Click 1" onClick="action.button1"/>\
            <button name="button2" title="Click 2" onClick="action.button2"/>\
          </panel>\
          <field name="other"/>\
        </panel>\
        </form>""";
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
