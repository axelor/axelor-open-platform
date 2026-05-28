/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.report;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.axelor.JpaTest;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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
