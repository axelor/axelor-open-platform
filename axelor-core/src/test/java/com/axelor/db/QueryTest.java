package com.axelor.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.axelor.BaseTest;
import com.axelor.db.Query;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Group;

public class QueryTest extends BaseTest {

	@Test
	public void testCount() {
		Assert.assertTrue(Group.all().count() > 0);
		Assert.assertTrue(Contact.all().count() > 0);
	}

	@Test
	public void testSimpleFilter() {
		Query<Contact> q = Contact.all().filter(
				"self.firstName < ? AND self.lastName LIKE ?", "some", "thing");
		System.err.println(q);
	}
	
	@Test
	public void testAdaptInt() {
		Contact.all().filter("self.id = ?", 1).fetchOne();
		Contact.all().filter("self.id IN (?, ?, ?)", 1, 2, 3).fetch();
	}
	
	@Test
	public void testAutoJoin() {
		Query<Contact> q = Contact.all().filter(
				"(self.addresses[].country.code = ?1 AND self.title.code = ?2) OR self.firstName = ?3",
				"FR", "MR", "John")
				.order("-addresses[].country.name");
		System.err.println(q);
		q.fetch();
	}
	
	@Test
	public void testJDBC() {
		
		JPA.jdbcWork(new JPA.JDBCWork() {
			
			@Override
			public void execute(Connection connection) throws SQLException {
				Statement stm = connection.createStatement();
				ResultSet rs = stm.executeQuery("SELECT * FROM contact_title");
				ResultSetMetaData meta = rs.getMetaData();
				while(rs.next()) {
					Map<String, Object> item = Maps.newHashMap();
					for(int i = 0 ; i < meta.getColumnCount() ; i++ )
						item.put(meta.getColumnName(i+1), rs.getObject(i+1));
					System.err.println(item);
				}
			}
		});
	}
}
