/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

import static org.junit.Assert.assertNotNull;

import javax.mail.Folder;
import javax.mail.Store;

import org.junit.Test;

import com.axelor.AbstractTest;

public class MailReaderTest extends AbstractTest {

	private static final String IMAP_HOST = "imap.gmail.com";
	private static final String IMAP_PORT = "993";
	
	private static final String IMAP_USER = "my.name@gmail.com";
	private static final String IMAP_PASS = "secret";

	@Test
	public void testRead() throws Exception {
		
		if ("secret".equals(IMAP_PASS)) {
			return;
		}
		
		MailAccount account = new ImapsAccount(IMAP_HOST, IMAP_PORT, IMAP_USER, IMAP_PASS);
		MailReader reader = new MailReader(account);
		
		Store store = reader.getStore();
		assertNotNull(store);
		
		Folder folder =	store.getFolder("INBOX");
		assertNotNull(folder);
	}
}
