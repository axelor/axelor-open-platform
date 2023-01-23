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
package com.axelor.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.common.ResourceUtils;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MailBuilderTest extends AbstractMailTest {

  private MailSender sender;

  @BeforeEach
  public void startServer() {
    if (sender == null) {
      sender = new MailSender(SMTP_ACCOUNT);
    }
  }

  private MimeMessage sendAndRecieve(Message message) throws Exception {
    sender.send(message);
    greenMail.waitForIncomingEmail(1);
    return greenMail.getReceivedMessages()[0];
  }

  @Test
  public void testPlain() throws Exception {

    Message message =
        sender
            .compose()
            .to(MY_EMAIL)
            .to("you@localhost", "else@localhost")
            .bcc("you@localhost")
            .subject("Hello...")
            .text("Hello!!!")
            .build();

    MimeMessage msg = sendAndRecieve(message);

    assertEquals(3, message.getRecipients(RecipientType.TO).length);
    assertEquals(1, message.getRecipients(RecipientType.BCC).length);

    assertTrue(msg.getContent() instanceof String);

    String content = (String) msg.getContent();

    assertEquals("Hello...", msg.getSubject());
    assertEquals("Hello!!!", content.trim());
  }

  @Test
  public void testMultipart() throws Exception {

    Message message =
        sender
            .compose()
            .to("aa@test.com")
            .bcc("you@localhost")
            .subject("Hello...")
            .text("Hello!!!")
            .text("World!!!")
            .build();

    MimeMessage msg = sendAndRecieve(message);

    assertEquals("Hello...", msg.getSubject());
    assertTrue(msg.getContent() instanceof Multipart);

    Multipart content = (Multipart) msg.getContent();

    assertEquals(1, content.getCount());

    for (int i = 0; i < content.getCount(); i++) {
      BodyPart part = content.getBodyPart(i);
      assertTrue(part.getContent() instanceof String);
    }
  }

  @Test
  public void testAttachment() throws Exception {

    String file = ResourceUtils.getResource("logback.xml").getFile();
    String url = "file://" + file;

    Message message =
        sender
            .compose()
            .to(MY_EMAIL)
            .bcc("you@localhost")
            .subject("Hello...")
            .text("Hello!!!")
            .attach("logback.xml", file)
            .attach("logback.xml", url)
            .build();

    MimeMessage msg = sendAndRecieve(message);

    assertTrue(msg.getContent() instanceof Multipart);

    Multipart content = (Multipart) msg.getContent();

    assertEquals(3, content.getCount());

    for (int i = 1; i < content.getCount(); i++) {
      BodyPart part = content.getBodyPart(i);
      assertNotNull(part.getFileName());
      assertNotNull(part.getContent());
    }
  }
}
