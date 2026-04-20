/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.db.internal.DBHelper;
import com.axelor.inject.Beans;
import com.axelor.script.ScriptTest;
import com.axelor.test.db.Contact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.persist.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SelectorTest extends ScriptTest {

  private Map firstByEmail(String email, String... fields) {
    List<Map> rows =
        Query.of(Contact.class)
            .filter("self.email = :email")
            .bind("email", email)
            .select(fields)
            .fetch(0, 0);
    assertEquals(1, rows.size(), "expected a single row for email " + email);
    return rows.get(0);
  }

  @Override
  @BeforeEach
  @Transactional
  public void prepare() {
    super.prepare();
    contact.setAttrs(getCustomerAttrsJson());
    contact.setAnotherAttrs(getCustomerAnotherAttrsJson());
    JPA.save(contact);
  }

  @Test
  void testSelectDefaultIdAndVersion() {
    // Empty select still returns id and version seeded by the Selector
    List<Map> rows = Query.of(Contact.class).order("id").select().fetch(0, 0);
    assertFalse(rows.isEmpty());
    Map first = rows.get(0);
    assertEquals(2, first.size());
    assertTrue(first.containsKey("id"));
    assertTrue(first.containsKey("version"));
  }

  @Test
  void testSelectScalarFields() {
    Map row = firstByEmail("jsmith@gmail.com", "firstName", "lastName");
    assertEquals(4, row.size()); // id, version, firstName, lastName
    assertEquals("John", row.get("firstName"));
    assertEquals("Smith", row.get("lastName"));
  }

  @Test
  void testSelectM2OReturnsCompactMap() {
    Map row = firstByEmail("jsmith@gmail.com", "title");
    assertEquals(3, row.size()); // id, version, title
    Map<String, Object> title = (Map<String, Object>) row.get("title");
    assertNotNull(title);
    assertEquals(3, title.size());
    assertTrue(title.containsKey("id"));
    assertTrue(title.containsKey("$version"));
    assertEquals("Mr.", title.get("name"));
    // the flat helper keys are not leaked
    assertFalse(row.containsKey("title.id"));
    assertFalse(row.containsKey("title.version"));
    assertFalse(row.containsKey("title.name"));
  }

  @Test
  void testSelectM2OSubFieldOnly() {
    // title.code without title → flat scalar, no compact map
    Map row = firstByEmail("jsmith@gmail.com", "title.code");
    assertEquals(3, row.size()); // id, version, title.code
    assertEquals("mr", row.get("title.code"));
    assertFalse(row.containsKey("title"));
  }

  @Test
  void testSelectM2OAndSubField() {
    // Matches the original bug report: both compact map and explicit scalar
    Map row = firstByEmail("jsmith@gmail.com", "title", "title.code");
    assertEquals(4, row.size()); // id, version, title, title.code
    Map<String, Object> title = (Map<String, Object>) row.get("title");
    assertEquals(3, title.size());
    assertEquals("Mr.", title.get("name"));
    assertEquals("mr", row.get("title.code"));
  }

  @Test
  void testSelectM2OSubScalarIsFlat() {
    // title.id alone is a flat Long, not a compact map
    Map row = firstByEmail("jsmith@gmail.com", "title.id");
    assertEquals(3, row.size());
    Object titleId = row.get("title.id");
    assertNotNull(titleId);
    assertFalse(titleId instanceof Map);
  }

  @Test
  void testSelectNullReference() {
    String email = "notitle-" + System.nanoTime() + "@test.com";
    JPA.runInTransaction(
        () -> {
          Contact c = new Contact();
          c.setFirstName("NoTitle");
          c.setLastName("Test");
          c.setEmail(email);
          JPA.save(c);
        });

    Map row = firstByEmail(email, "title");
    assertTrue(row.containsKey("title"));
    assertNull(row.get("title"));
  }

  @Test
  void testSelectBinaryFieldIgnored() {
    // image is byte[] (BINARY) and must be omitted from the query
    String jpql = Query.of(Contact.class).select("image").toString();
    assertFalse(jpql.contains("image"));
  }

  @Test
  void testSelectUnknownFieldIgnored() {
    // unknown names are silently dropped
    String jpql = Query.of(Contact.class).select("noSuchField").toString();
    assertFalse(jpql.contains("noSuchField"));
  }

  @Test
  void testSelectOneToManyCollection() {
    // O2M collections are loaded via fetchCollections → List of compact maps
    Map row = firstByEmail("jtaylor@gmail.com", "addresses");
    List<?> addresses = (List<?>) row.get("addresses");
    assertNotNull(addresses);
    assertEquals(2, addresses.size());
    Map<?, ?> addr = (Map<?, ?>) addresses.get(0);
    assertTrue(addr.containsKey("id"));
  }

  @Test
  void testSelectManyToManyCollection() {
    Map row = firstByEmail("jsmith@gmail.com", "circles");
    List<?> circles = (List<?>) row.get("circles");
    assertNotNull(circles);
    assertEquals(2, circles.size());
  }

  @Test
  void testLimitAndOffset() {
    List<Map> page1 = Query.of(Contact.class).order("id").select("firstName").fetch(2, 0);
    List<Map> page2 = Query.of(Contact.class).order("id").select("firstName").fetch(2, 2);
    assertEquals(2, page1.size());
    assertTrue(page2.size() <= 2);
    Set<Object> page1Ids = page1.stream().map(m -> m.get("id")).collect(Collectors.toSet());
    for (Map m : page2) {
      assertFalse(page1Ids.contains(m.get("id")), "pages must not overlap");
    }
  }

  @Test
  void testValuesReturnsRawList() {
    List<List> raws =
        Query.of(Contact.class)
            .filter("self.email = :email")
            .bind("email", "jsmith@gmail.com")
            .select("firstName")
            .values(0, 0);
    assertEquals(1, raws.size());
    List first = raws.get(0);
    // seeded id, version, then firstName → 3 entries
    assertEquals(3, first.size());
    assertEquals("John", first.get(2));
  }

  @Test
  void testValuesReturnsM2OScalarsNotEntity() {
    // values() returns the raw JPQL row: for an m2o field, three scalars
    // (id, version, nameField) take the place of what used to be a single
    // full-entity slot. No Title instance is materialized.
    List<List> raws =
        Query.of(Contact.class)
            .filter("self.email = :email")
            .bind("email", "jsmith@gmail.com")
            .select("firstName", "title", "title.code")
            .values(0, 0);
    assertEquals(1, raws.size());
    List row = raws.get(0);
    // seeded id, version, firstName, title.id, title.version, title.name, title.code
    assertEquals(7, row.size());
    assertEquals("John", row.get(2));
    assertNotNull(row.get(3)); // title.id
    assertEquals(0, row.get(4)); // title.version
    assertEquals("Mr.", row.get(5)); // title.name
    assertEquals("mr", row.get(6)); // title.code
    for (Object value : row) {
      assertFalse(
          value instanceof com.axelor.test.db.Title,
          "values() must not contain a full Title entity — got " + value);
    }
  }

  @Test
  void testToStringReturnsJpql() {
    String jpql = Query.of(Contact.class).select("firstName").toString();
    assertTrue(jpql.startsWith("SELECT"));
    assertTrue(jpql.contains("FROM Contact self"));
    assertTrue(jpql.contains("self.firstName"));
  }

  @Test
  void testM2OJpqlHasNoBareAlias() {
    // Regression: the m2o alias must NOT appear as a bare selected column;
    // only its id/version/nameField scalars should be fetched.
    String jpql = Query.of(Contact.class).select("title").toString();
    assertTrue(jpql.contains("_title.id"));
    assertTrue(jpql.contains("_title.name"));

    int start = jpql.indexOf("new List(");
    int end = jpql.indexOf(")", start);
    String items = jpql.substring(start + "new List(".length(), end);
    for (String item : items.split(",")) {
      assertNotEquals(
          "_title", item.trim(), "SELECT must not contain the bare _title alias: " + jpql);
    }
  }

  @Test
  void testSelectWithFilter() {
    List<Map> rows =
        Query.of(Contact.class)
            .filter("self.firstName = :first")
            .bind("first", "John")
            .select("firstName", "email")
            .fetch(0, 0);
    assertEquals(1, rows.size());
    assertEquals("John", rows.get(0).get("firstName"));
    assertEquals("jsmith@gmail.com", rows.get(0).get("email"));
  }

  @Test
  void testSelectWithOrder() {
    List<Map> rows = Query.of(Contact.class).order("firstName").select("firstName").fetch(0, 0);
    for (int i = 1; i < rows.size(); i++) {
      String prev = (String) rows.get(i - 1).get("firstName");
      String curr = (String) rows.get(i).get("firstName");
      assertTrue(prev.compareTo(curr) <= 0, "results must be ordered by firstName");
    }
  }

  @Test
  void testSelectDuplicateIdIsIdempotent() {
    // Passing "id" explicitly is harmless — the key stays single-valued
    Map row = firstByEmail("jsmith@gmail.com", "id", "firstName");
    assertNotNull(row.get("id"));
    assertEquals("John", row.get("firstName"));
  }

  @Test
  void testSelectDedupesRepeatedNames() {
    // Duplicate names (seeded id/version included) must not produce duplicate columns
    String jpql =
        Query.of(Contact.class).select("id", "version", "firstName", "firstName").toString();
    int start = jpql.indexOf("new List(");
    int end = jpql.indexOf(")", start);
    String[] items = jpql.substring(start + "new List(".length(), end).split(",");
    long idCount = java.util.Arrays.stream(items).filter(s -> s.trim().equals("self.id")).count();
    long versionCount =
        java.util.Arrays.stream(items).filter(s -> s.trim().equals("self.version")).count();
    long firstNameCount =
        java.util.Arrays.stream(items).filter(s -> s.trim().equals("self.firstName")).count();
    assertEquals(1, idCount, "self.id must appear exactly once");
    assertEquals(1, versionCount, "self.version must appear exactly once");
    assertEquals(1, firstNameCount, "self.firstName must appear exactly once");
  }

  @Test
  void testSelectJsonField() throws JsonProcessingException {
    Assumptions.assumeTrue(DBHelper.isPostgreSQL());
    Map row =
        firstByEmail(
            "jsmith@gmail.com",
            "id",
            "firstName",
            "attrs",
            "attrs.nickName",
            "attrs.guardian.id",
            "anotherAttrs.nickName");
    assertEquals(7, row.size());
    Map attrs = Beans.get(ObjectMapper.class).readValue((String) row.get("attrs"), Map.class);
    assertEquals(8, attrs.size());
    assertEquals("Some Name", attrs.get("nickName"));
    assertEquals("Some Name", row.get("attrs.nickName"));
    assertEquals("1", row.get("attrs.guardian.id"));
    assertEquals("Some Custom Name", row.get("anotherAttrs.nickName"));
  }
}
