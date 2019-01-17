/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.mail;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import org.junit.Assert;
import org.junit.Test;

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

    Assert.assertEquals(html, parser.getHtml());
    Assert.assertNotNull(parser.getText());
    Assert.assertNotNull(parser.getSummary());

    Assert.assertTrue(parser.getText().startsWith("This is a test.."));
    Assert.assertTrue(parser.getText().endsWith("John Smith"));
    Assert.assertTrue(parser.getSummary().equals("This is a test.."));
  }
}
