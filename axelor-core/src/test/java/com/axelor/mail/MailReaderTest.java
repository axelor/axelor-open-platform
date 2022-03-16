/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.mail.Folder;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

public class MailReaderTest extends AbstractMailTest {

  final MailSender sender = new MailSender(SMTP_ACCOUNT);
  final MailReader imapReader = new MailReader(IMAP_ACCOUNT);
  final MailReader pop3Reader = new MailReader(POP3_ACCOUNT);

  private void test(MailSender sender, MailReader reader) throws Exception {

    final MimeMessage msg =
        sender
            .compose()
            .from(MY_EMAIL)
            .to("you@localhost")
            .subject("Hello...")
            .text("Hello!!!")
            .build();

    greenMail.getUserManager().getUser(USER_NAME).deliver(msg);
    greenMail.waitForIncomingEmail(1);

    Store store = reader.getStore();
    assertNotNull(store);

    Folder folder = store.getFolder("INBOX");
    assertNotNull(folder);

    folder.open(Folder.READ_ONLY);

    assertEquals(1, folder.getMessageCount());

    MimeMessage incoming = (MimeMessage) folder.getMessage(1);
    assertNotNull(incoming);
    assertEquals("Hello...", incoming.getSubject());

    MimeMessage reply = (MimeMessage) incoming.reply(false);
    reply.setText("This is a reply...");

    greenMail.getUserManager().getUser(USER_NAME).deliver(reply);
    greenMail.waitForIncomingEmail(1);

    folder = store.getFolder("INBOX");
    folder.open(Folder.READ_ONLY);

    assertEquals(2, folder.getMessageCount());

    incoming = (MimeMessage) folder.getMessage(2);

    assertEquals("Re: Hello...", incoming.getSubject());
    assertEquals(msg.getMessageID(), incoming.getHeader("In-Reply-To", ""));
  }

  @Test
  public void testIMAP() throws Exception {
    test(sender, imapReader);
  }

  @Test
  public void testPOP3() throws Exception {
    test(sender, pop3Reader);
  }
}
