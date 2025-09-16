/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.common.ResourceUtils;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
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
