/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class AbstractMailTest {

  protected static final String MY_EMAIL = "me@localhost";
  protected static final String USER_NAME = "test";
  protected static final String USER_PASS = "test";

  @RegisterExtension
  GreenMailExtension greenMail =
      new GreenMailExtension(ServerSetupTest.SMTP_POP3_IMAP)
          .withConfiguration(GreenMailConfiguration.aConfig().withUser(USER_NAME, USER_PASS));

  protected final SmtpAccount SMTP_ACCOUNT =
      new SmtpAccount(
          ServerSetup.getLocalHostAddress(),
          String.valueOf(ServerSetupTest.SMTP.getPort()),
          USER_NAME,
          USER_PASS);
  protected final ImapAccount IMAP_ACCOUNT =
      new ImapAccount(
          ServerSetup.getLocalHostAddress(),
          String.valueOf(ServerSetupTest.IMAP.getPort()),
          USER_NAME,
          USER_PASS);
  protected final Pop3Account POP3_ACCOUNT =
      new Pop3Account(
          ServerSetup.getLocalHostAddress(),
          String.valueOf(ServerSetupTest.POP3.getPort()),
          USER_NAME,
          USER_PASS);
}
