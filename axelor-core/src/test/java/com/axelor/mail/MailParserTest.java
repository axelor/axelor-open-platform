/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

public class MailParserTest {

  private String html =
      """
      <div>This is a test..</div><br><br><p>\
      This is a link: <a href="http://example.com/link.html">Some Link</a></p><br>\
      Regards <br>--<div>John Smith</div>""";

  @Test
  public void testPlainText() throws Exception {

    final MimeMessage message = new MimeMessage((Session) null);
    message.setText(html, "UTF-8", "html");
    message.saveChanges();

    final MailParser parser = new MailParser(message).parse();

    assertEquals(html, parser.getHtml());
    assertNotNull(parser.getText());
    assertNotNull(parser.getSummary());

    assertTrue(parser.getText().startsWith("This is a test.."));
    assertTrue(parser.getText().endsWith("John Smith"));
    assertTrue(parser.getSummary().equals("This is a test.."));
  }
}
