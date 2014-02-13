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
package com.axelor.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.axelor.JpaTest;
import com.axelor.test.db.Circle;
import com.axelor.test.db.Contact;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

public class QueryTest extends JpaTest {

	@Test
	public void testCount() {
		Assert.assertTrue(Circle.all().count() > 0);
		Assert.assertTrue(Contact.all().count() > 0);
	}

	@Test
	public void testSimpleFilter() {

		String filter = "self.firstName < ? AND self.lastName LIKE ?";
		String expected = "SELECT self FROM Contact self WHERE self.firstName < ? AND self.lastName LIKE ?";

		Query<Contact> q = Contact.all().filter(filter, "some", "thing");

		Assert.assertEquals(expected, q.toString());
	}

	@Test
	public void testAdaptInt() {
		Contact.all().filter("self.id = ?", 1).fetchOne();
		Contact.all().filter("self.id IN (?, ?, ?)", 1, 2, 3).fetch();
	}

	@Test
	public void testAutoJoin() {

		String filter = "(self.addresses[].country.code = ?1 AND self.title.code = ?2) OR self.firstName = ?3";
		String expected = "SELECT self FROM Contact self "
				+ "LEFT JOIN self.addresses _addresses "
				+ "LEFT JOIN _addresses.country _addresses_country "
				+ "LEFT JOIN self.title _title "
				+ "WHERE (_addresses_country.code = ?1 AND _title.code = ?2) OR self.firstName = ?3 "
				+ "ORDER BY _addresses_country.name DESC";

		Query<Contact> q = Contact.all().filter(filter, "FR", "MR", "John").order("-addresses[].country.name");
		Assert.assertEquals(expected, q.toString());

		List<?> result = q.fetch();	
		Assert.assertNotNull(result);
		Assert.assertTrue(result.size() > 0);
	}

	@Test
	@Transactional
	public void testBulkUpdate() {
		Query<Contact> q = Contact.all().filter("self.title.code = ?1", "mr");
		for (Contact c : q.fetch()) {
			Assert.assertNull(c.getLang());
		}
		// managed instances are not affected with mass update
		// so clear the session to avoid unexpected results
		JPA.clear();
		q.update("self.lang", "EN");
		for (Contact c : q.fetch()) {
			Assert.assertEquals("EN", c.getLang());
		}
	}

	@Test
	public void testJDBC() {

		JPA.jdbcWork(new JPA.JDBCWork() {

			@Override
			public void execute(Connection connection) throws SQLException {
				Statement stm = connection.createStatement();
				ResultSet rs = stm.executeQuery("SELECT * FROM contact_title");
				ResultSetMetaData meta = rs.getMetaData();
				while (rs.next()) {
					Map<String, Object> item = Maps.newHashMap();
					for (int i = 0; i < meta.getColumnCount(); i++) {
						item.put(meta.getColumnName(i + 1), rs.getObject(i + 1));
					}
					Assert.assertFalse(item.isEmpty());
				}
			}
		});
	}
}
