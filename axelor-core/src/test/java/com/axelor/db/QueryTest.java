/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.common.ObjectUtils;
import com.axelor.rpc.Context;
import com.axelor.rpc.ContextEntity;
import com.axelor.script.ScriptTest;
import com.axelor.test.db.Address;
import com.axelor.test.db.Circle;
import com.axelor.test.db.Contact;
import com.google.inject.persist.Transactional;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.Test;

public class QueryTest extends ScriptTest {

  @Test
  public void testCount() {
    assertTrue(all(Circle.class).count() > 0);
    assertTrue(all(Contact.class).count() > 0);
  }

  @Test
  public void testQueryPathComparison() {
    // comparing an entity path against a literal
    assertTrue(all(Address.class).filter("self.country = ?1", 1).count() > 0);

    // comparing an entity path against a proxy instance
    final Context context = new Context(contextMap(), Contact.class);
    final Contact proxy = context.asType(Contact.class);

    assertInstanceOf(ContextEntity.class, proxy);
    assertTrue(all(Address.class).filter("self.contact = ?1", proxy).count() > 0);
  }

  @Test
  public void testSimpleFilter() {

    String filter = "self.firstName < ? AND self.lastName LIKE ?";
    String expected =
        "SELECT self FROM Contact self WHERE self.firstName < ?1 AND self.lastName LIKE ?2";

    Query<Contact> q = all(Contact.class).filter(filter, "some", "thing");

    assertEquals(expected, q.toString());
  }

  @Test
  public void testAdaptInt() {
    all(Contact.class).filter("self.id = ?", 1).fetchOne();
    all(Contact.class).filter("self.id IN (?, ?, ?)", 1, 2, 3).fetch();
  }

  @Test
  public void testAutoJoin() {

    String filter =
        "(self.addresses[].country.code = ?1 AND self.title.code = ?2) OR self.firstName = ?3";
    String expected =
        """
        SELECT DISTINCT self FROM Contact self \
        LEFT JOIN FETCH self.addresses _addresses \
        LEFT JOIN FETCH _addresses.country _addresses_country \
        LEFT JOIN self.title _title \
        WHERE (_addresses_country.code = ?1 AND _title.code = ?2) OR self.firstName = ?3 \
        ORDER BY _addresses_country.name DESC""";

    Query<Contact> q =
        all(Contact.class).filter(filter, "FR", "MR", "John").order("-addresses[].country.name");
    assertEquals(expected, q.toString());

    List<?> result = q.fetch();
    assertNotNull(result);
    assertTrue(result.size() > 0);
  }

  @Test
  public void testDistinct() {
    final String filter =
        """
        self.addresses.country.code IS NOT NULL \
        AND self.title.code IS NOT NULL \
        OR self.firstName IS NOT NULL""";
    final List<Contact> resultList = all(Contact.class).filter(filter).fetch();
    final Set<Contact> resultSet = new HashSet<>(resultList);
    assertEquals(resultSet.size(), resultList.size(), "Results should be unique.");

    final List<Contact> orderedResultList =
        all(Contact.class).filter(filter).order("-addresses.country.name").fetch();
    final Set<Contact> orderedResultSet = new HashSet<>(orderedResultList);
    assertEquals(
        orderedResultSet.size(), orderedResultList.size(), "Ordered results should be unique.");

    assertEquals(
        resultList.size(),
        orderedResultList.size(),
        "Ordering should not change number of results.");

    final long count = all(Contact.class).filter(filter).count();
    assertEquals(resultList.size(), count, "Counting should be consistent with number of results.");

    @SuppressWarnings("rawtypes")
    final List<Map> selectResults =
        all(Contact.class)
            .filter(filter)
            .order("-addresses.country.name")
            .select("fullName", "email")
            .fetch(0, 0);

    assertEquals(
        resultList.size(),
        selectResults.size(),
        "Selecting fields should be consistent with number of results.");
  }

  @Test
  @Transactional
  public void testStream() {
    final Query<Contact> q = all(Contact.class);
    final List<?> first = q.fetch();
    final List<?> second;
    try (final Stream<?> stream = q.fetchStream()) {
      second = stream.collect(Collectors.toList());
    }
    assertEquals(first.size(), second.size());
  }

  @Test
  @Transactional
  public void testBulkRemove() {
    final List<String> names = Arrays.asList("Bulk Remove 1", "Bulk Remove 2");
    names.stream()
        .forEach(
            name -> {
              Contact c = new Contact();
              c.setFirstName(name);
              c.setLastName(name);
              JPA.em().persist(c);
            });
    final Query<Contact> q =
        all(Contact.class).filter("self.firstName in (:names)").bind("names", names);
    final long count = q.count();
    final long removed = q.remove();
    assertEquals(count, removed);
  }

  @Test
  @Transactional
  public void testBulkUpdate() {
    Query<Contact> q = all(Contact.class).filter("self.title.code = ?1", "mr");
    for (Contact c : q.fetch()) {
      assertNull(c.getLang());
    }

    final String lang = "EN";
    final String food = "pizza";

    // Update one field
    q.update("self.lang", lang);

    // managed instances are not affected with mass update
    // so clear the session to avoid unexpected results
    getEntityManager().clear();

    for (Contact c : q.fetch()) {
      assertEquals(lang, c.getLang());
    }

    // Update several fields
    q.update(Map.of("self.lang", lang, "self.food", food));

    getEntityManager().clear();

    for (Contact c : q.fetch()) {
      assertEquals(lang, c.getLang());
      assertEquals(food, c.getFood());
    }

    q.update("self.lang", null);
    q.update("self.food", null);

    getEntityManager().clear();

    for (Contact c : q.fetch()) {
      assertEquals(null, c.getLang());
      assertEquals(null, c.getFood());
    }
  }

  @Test
  public void testJDBC() {

    jdbcTask(
        new JDBCTask() {

          @Override
          public void execute(Connection connection) throws SQLException {
            try (Statement stm = connection.createStatement()) {
              try (ResultSet rs = stm.executeQuery("SELECT * FROM contact_title")) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                  Map<String, Object> item = new HashMap<>();
                  for (int i = 0; i < meta.getColumnCount(); i++) {
                    item.put(meta.getColumnName(i + 1), rs.getObject(i + 1));
                  }
                  assertFalse(item.isEmpty());
                }
              }
            }
          }
        });
  }

  @Test
  void testFilterAdaptSingle() {
    var filter = "self.credit = :credit";
    var credit = "2.5";
    var results = all(Contact.class).filter(filter).bind("credit", credit).fetch();
    assertEquals(1, results.size());
  }

  @Test
  void testFilterAdaptCollection() {
    var filter = "self.credit IN :credits";
    var credits = new ArrayList<String>();
    credits.add(null);
    credits.add("");
    credits.add("2.5");
    var results = all(Contact.class).filter(filter).bind("credits", credits).fetch();
    assertEquals(3, results.size());
  }

  @Test
  void testFilterNullKeyInCollection() {
    var filter = "self.id IN :ids";
    var ids = new ArrayList<Object>();
    ids.add(null);
    ids.add("");
    ids.add(1L);
    ids.add("2");

    // Null values are filtered out, otherwise they cause AssertionError when caching is enabled.
    var results = all(Contact.class).filter(filter).bind("ids", ids).cacheable().fetch();

    assertEquals(2, results.size());
  }

  @Test
  void testFilterProxy() {
    var contact = all(Contact.class).filter("self.title.code = ?", "mr").fetchOne();
    var title = contact.getTitle();
    assertInstanceOf(HibernateProxy.class, title);

    var resultsEquals =
        all(Contact.class).filter("self.title = :title").bind("title", title).fetch();
    assertEquals(2, resultsEquals.size());

    var resultsIn =
        all(Contact.class).filter("self.title IN :titles").bind("titles", List.of(title)).fetch();
    assertEquals(2, resultsIn.size());
  }

  @Test
  void testQueryAdaptSingle() {
    var qlString = "SELECT self FROM Contact self WHERE self.credit = :credit";
    var credit = "2.5";
    var query = JPA.em().createQuery(qlString, Contact.class);

    // Hibernate 5 throws IllegalArgumentException.
    // Hibernate 6 can coerce single value.
    query.setParameter("credit", credit);

    var results = query.getResultList();
    assertEquals(1, results.size());
  }

  @Test
  void testQueryAdaptCollection() {
    var qlString = "SELECT self FROM Contact self WHERE self.credit IN :credits";
    var credits = new ArrayList<String>();
    credits.add(null);
    credits.add("");
    credits.add("2.5");
    var query = JPA.em().createQuery(qlString, Contact.class);

    // Hibernate 5 throws IllegalArgumentException.
    // Hibernate 6 cannot coerce multi value and does not throw IllegalArgumentException.
    query.setParameter("credits", credits);

    assertThrows(NumberFormatException.class, query::getResultList);
  }

  @Test
  void testQueryNullKeyInCollection() {
    var qlString = "SELECT self FROM Contact self WHERE self.id IN :ids";
    var ids = new ArrayList<Long>();
    ids.add(null);
    ids.add(1L);
    ids.add(2L);
    var query = JPA.em().createQuery(qlString, Contact.class);
    query.setHint(AvailableHints.HINT_CACHEABLE, true);
    query.setParameter("ids", ids);

    // Hibernate 5 doesn't fail because of null in collection.
    // Hibernate 6 throws AssertionError because of null in collection when caching is enabled.
    assertThrows(AssertionError.class, query::getResultList);
  }

  @Test
  void testQueryProxy() {
    var contact = all(Contact.class).filter("self.title.code = ?", "mr").fetchOne();
    var title = contact.getTitle();
    assertInstanceOf(HibernateProxy.class, title);

    var resultsEquals =
        JPA.em()
            .createQuery("SELECT self FROM Contact self WHERE self.title = :title", Contact.class)
            .setParameter("title", title)
            .getResultList();
    assertEquals(2, resultsEquals.size());

    var resultsIn =
        JPA.em()
            .createQuery("SELECT self FROM Contact self WHERE self.title IN :titles", Contact.class)
            .setParameter("titles", List.of(title))
            .getResultList();
    assertEquals(2, resultsIn.size());
  }

  /** Hibernate 6's stricter parser assigns a single parameter type per parameter. */
  @Test
  void testQueryAmbiguousParameter() {
    // firstNames is parsed as String instead of Collection<String>
    var qlString =
        """
        SELECT self FROM Contact self
        WHERE :firstNames IS NULL OR self.firstName IN :firstNames
        """;
    var query = JPA.em().createQuery(qlString, Contact.class);
    var firstNames = List.of("Jane", "John");
    var firstNamesParam = ObjectUtils.isEmpty(firstNames) ? null : firstNames;

    assertThrows(
        IllegalArgumentException.class, () -> query.setParameter("firstNames", firstNamesParam));
  }

  @Test
  void testQueryNonAmbiguousParameter() {
    var qlString =
        """
        SELECT self FROM Contact self
        WHERE :isFirstNamesEmpty = TRUE OR self.firstName IN :firstNames
        """;
    var query = JPA.em().createQuery(qlString, Contact.class);
    var firstNames = List.of("Jane", "John");
    var isFirstNamesEmpty = ObjectUtils.isEmpty(firstNames);

    query.setParameter("isFirstNamesEmpty", isFirstNamesEmpty);
    query.setParameter("firstNames", firstNames);

    var results = query.getResultList();
    assertEquals(1, results.size());
  }

  @Test
  void testQueryParamType() {
    var em = JPA.em();

    assertDoesNotThrow(
        () ->
            em.createQuery("SELECT self FROM Contact self WHERE self.id = :id", Contact.class)
                .setParameter("id", 1L),
        "Named parameter with Long type should succeed");

    assertDoesNotThrow(
        () ->
            em.createQuery("SELECT self FROM Contact self WHERE self.id = :id", Contact.class)
                .setParameter("id", "1"),
        "Named parameter with String type should be coerced and succeed since Hibernate 6");

    assertDoesNotThrow(
        () -> em.createQuery("SELECT self FROM Contact self WHERE self.id = 1", Contact.class),
        "Numeric literal should succeed");

    assertThrows(
        IllegalArgumentException.class,
        () -> em.createQuery("SELECT self FROM Contact self WHERE self.id = '1'", Contact.class),
        "Comparing numeric to string literal should fail since Hibernate 6");
  }
}
