/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.report;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.axelor.JpaTest;
import com.axelor.db.JPA;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.HashMap;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.model.api.IResourceLocator;
import org.junit.jupiter.api.Test;

@GuiceModules(MyModule.class)
public class ReportTest extends JpaTest {

  private static final String DESIGN = "contacts.rptdesign";

  private static final String SESSION_DESIGN = "contacts-session.rptdesign";

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
    String html = render(DESIGN);

    for (Contact contact : contacts.all().fetch()) {
      assertTrue(html.contains(contact.getFullName()));
    }
  }

  @Test
  public void testRenderWithEntityQueries() {
    // entity queries run while rendering must not close the connection used by BIRT
    String html = render(SESSION_DESIGN);
    assertTrue(html.contains("Contact count: " + contacts.all().count()), html);
  }

  @Test
  public void testRenderInTransaction() {
    // within a transaction, the report sees its uncommitted changes
    JPA.runInTransaction(
        () -> {
          Contact contact = JPA.save(new Contact("Uncommitted", "Contact"));
          JPA.flush();
          String html = render(DESIGN);
          JPA.em().getTransaction().setRollbackOnly();
          assertTrue(html.contains(contact.getFullName()), html);
        });
  }

  private String render(String design) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      generator.generate(output, design, "html", new HashMap<>());
    } catch (Exception e) {
      fail(e);
    }
    return output.toString();
  }
}
