/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.common.ResourceUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class MailSenderTest extends AbstractMailTest {

  private static final String SMTP_HOST = "smtp.gmail.com";
  private static final String SMTP_PORT = "587";

  private static final String SMTP_USER = "my.name@gmail.com";
  private static final String SMTP_PASS = "secret";

  private static final String SEND_TO = "my.name@gmail.com";

  private static final String HTML =
      """
      <strong>Hello world...</strong>\
      <hr>\
      <p>This is a testing email and not a <strong><span style='color: red;'>spam...</span></strong></p>\
      <p>This is logo1...</p>\
      <img src='cid:logo1.png'></img>\
      <br>\
      ---\
      <span style='color: blue;'><i>John Smith</i></span>""";

  private static final String TEXT =
      """
      Hello world...
      --------------

      This is a testing email and not a *spam...*

      ---
      John Smith""";

  private void send(MailAccount account, Date sentOn) throws MessagingException, IOException {

    final MailSender sender = new MailSender(account);

    final String file = ResourceUtils.getResource("com/axelor/mail/test-file.txt").getFile();
    final String image = ResourceUtils.getResource("com/axelor/mail/test-image.png").getFile();
    final String imageData =
        "data:image/png;base64,"
            + Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of(image)));
    final String html = HTML + "<br><img src='" + imageData + "' title='test-image.png'>";

    sender
        .compose()
        .to(SEND_TO)
        .subject("Hello...")
        .text(TEXT)
        .html(html)
        .attach("text.txt", file)
        .inline("logo1.png", image)
        .send(sentOn);
  }

  @Test
  public void testReal() throws Exception {
    if ("secret".equals(SMTP_PASS)) {
      return;
    }
    final MailAccount account =
        new SmtpAccount(SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, MailConstants.CHANNEL_STARTTLS);
    send(account, null);
  }

  @Test
  public void testLocal() throws Exception {

    final Date sentOn = Date.from(LocalDateTime.now().minusDays(15).toInstant(ZoneOffset.UTC));

    send(SMTP_ACCOUNT, sentOn);

    assertNotNull(greenMail.getReceivedMessages());
    assertTrue(greenMail.getReceivedMessages().length > 0);

    final MimeMessage m1 = greenMail.getReceivedMessages()[0];

    assertNotNull(m1);
    assertEquals("Hello...", m1.getSubject());
    assertTrue(sentOn.compareTo(m1.getSentDate()) >= 0);
    assertTrue(m1.getContent() instanceof MimeMultipart);

    final MimeMultipart parts = (MimeMultipart) m1.getContent();

    assertEquals(2, parts.getCount());

    // test multipart/related
    final MimeBodyPart part1 = (MimeBodyPart) parts.getBodyPart(0);
    assertTrue(part1.getContentType().contains("multipart/related"));
    assertTrue(part1.getContent() instanceof MimeMultipart);

    final MimeMultipart related = (MimeMultipart) part1.getContent();

    assertEquals(3, related.getCount());
    assertTrue(related.getBodyPart(0).getContent() instanceof MimeMultipart);

    final MimeMultipart alternative = (MimeMultipart) related.getBodyPart(0).getContent();
    assertEquals(3, related.getCount());

    final MimeBodyPart textPart = (MimeBodyPart) alternative.getBodyPart(0);
    final MimeBodyPart htmlPart = (MimeBodyPart) alternative.getBodyPart(1);

    assertTrue(textPart.getContent() instanceof String);
    assertTrue(htmlPart.getContent() instanceof String);

    final String text = (String) textPart.getContent();
    final String html = (String) htmlPart.getContent();

    assertEquals(TEXT.trim().replace("\n", "\r\n"), text);
    assertTrue(html.contains("<strong>Hello world...</strong>"));
    assertTrue(html.contains("src=\"cid:image"));

    // test inline attachment
    final MimeBodyPart inline = (MimeBodyPart) related.getBodyPart(1);
    assertEquals("test-image.png", inline.getFileName());
    assertTrue(inline.getContentType().contains("image/png"));
    assertTrue(inline.getDisposition().contains("inline"));

    // test attachment
    final MimeBodyPart part2 = (MimeBodyPart) parts.getBodyPart(1);
    assertEquals("text.txt", part2.getFileName());
    assertEquals("Hello...", part2.getContent());
  }
}
