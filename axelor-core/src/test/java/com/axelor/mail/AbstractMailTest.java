/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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

import java.security.Security;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.DummySSLSocketFactory;
import com.icegreen.greenmail.util.ServerSetupTest;

public abstract class AbstractMailTest {

	protected static final String SERVER_HOST = "127.0.0.1";

	protected static final String SMTP_PORT = "" + ServerSetupTest.SMTP.getPort();
	protected static final String IMAP_PORT = "" + ServerSetupTest.IMAP.getPort();
	protected static final String POP3_PORT = "" + ServerSetupTest.POP3.getPort();

	protected static final String USER_NAME = "test";
	protected static final String USER_PASS = "test";

	@Rule
	public final GreenMailRule server = new GreenMailRule(ServerSetupTest.SMTP_POP3_IMAP);

	protected final SmtpAccount SMTP_ACCOUNT = new SmtpAccount(SERVER_HOST, SMTP_PORT, USER_NAME, USER_PASS);
	protected final ImapAccount IMAP_ACCOUNT = new ImapAccount(SERVER_HOST, IMAP_PORT, USER_NAME, USER_PASS);
	protected final Pop3Account POP3_ACCOUNT = new Pop3Account(SERVER_HOST, POP3_PORT, USER_NAME, USER_PASS);

	protected GreenMailUser user;

	@Before
	public void startServer() {
		Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory.class.getName());
		user = server.setUser(USER_NAME, USER_PASS);
		server.start();
	}

	@After
	public void stopServer() {
		server.stop();
	}
}
