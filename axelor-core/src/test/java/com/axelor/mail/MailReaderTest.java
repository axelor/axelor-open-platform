/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
