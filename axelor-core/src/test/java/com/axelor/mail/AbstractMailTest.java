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
