/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

public class MailParserTest {

  private String html =
      "<div>This is a test..</div><br><br><p>"
          + "This is a link: <a href=\"http://example.com/link.html\">Some Link</a></p><br>"
          + "Regards <br>--<div>John Smith</div>";

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
