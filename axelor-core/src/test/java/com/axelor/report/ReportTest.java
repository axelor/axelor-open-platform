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
package com.axelor.report;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.axelor.JpaTest;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.model.api.IResourceLocator;
import org.junit.jupiter.api.Test;

@GuiceModules(MyModule.class)
public class ReportTest extends JpaTest {

  private static final String DESIGN = "contacts.rptdesign";

  @Inject private IReportEngine engine;

  @Inject private ReportGenerator generator;

  @Inject private ContactRepository contacts;

  @Test
  public void testEngine() {
    assertNotNull(engine);
    assertNotNull(generator);
  }

  @Test
  public void testResourceLocator() {
    IResourceLocator locator = engine.getConfig().getResourceLocator();
    URL found = locator.findResource(null, DESIGN, IResourceLocator.OTHERS);
    assertNotNull(found);
  }

  @Test
  public void testRender() {

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Map<String, Object> params = new HashMap<>();

    try {
      try {
        generator.generate(output, DESIGN, "html", params);
      } finally {
        output.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    byte[] bytes = output.toByteArray();

    assertNotNull(bytes);
    assertTrue(bytes.length > 0);

    String html = new String(bytes);

    for (Contact contact : contacts.all().fetch()) {
      assertTrue(html.contains(contact.getFullName()));
    }
  }
}
