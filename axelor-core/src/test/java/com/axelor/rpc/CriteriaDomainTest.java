/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axelor.db.JPA;
import com.axelor.rpc.filter.Filter;
import com.axelor.rpc.filter.JPQLFilter;
import com.axelor.test.db.Contact;
import com.google.inject.persist.Transactional;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** A client domain is ANDed with the permission filter and must not get past it. */
class CriteriaDomainTest extends RpcTest {

  private static final String ESCAPING_DOMAIN = "1=1)) OR ((1=1";

  private Filter permission;

  @BeforeEach
  @Transactional
  void createContacts() {
    for (var i = 0; i < 3; i++) {
      var contact = new Contact();
      contact.setFirstName("Domain" + i);
      contact.setLastName("Test" + i);
      JPA.save(contact);
    }
    // stands in for the row-level filter JpaSecurity gives: one row
    permission = new JPQLFilter("self.id = ?", JPA.all(Contact.class).fetchOne().getId());
  }

  private static Request request(Map<String, Object> data) {
    var request = new Request();
    request.setModel(Contact.class.getName());
    request.setData(data);
    return request;
  }

  @Test
  void rejectsDomainEscapingPermissionFilter() {
    var request = request(Map.of("_domain", ESCAPING_DOMAIN));
    assertThrows(IllegalArgumentException.class, request::getCriteria);
  }

  @Test
  void rejectsDomainsEntryEscapingPermissionFilter() {
    var request =
        request(
            Map.of("criteria", List.of(), "_domains", List.of(Map.of("domain", ESCAPING_DOMAIN))));
    assertThrows(IllegalArgumentException.class, request::getCriteria);
  }

  @Test
  void rejectsDomainsEntriesEscapingPermissionFilterViaSplitComment() {
    var request =
        request(
            Map.of(
                "criteria",
                List.of(),
                "_domains",
                List.of(
                    Map.of("domain", "1=1 /*"),
                    Map.of("domain", "' */ )) OR ((1=1 AND /* '"),
                    Map.of("domain", "*/ 1=1"))));
    assertThrows(IllegalArgumentException.class, request::getCriteria);
  }

  @Test
  void rejectsDomainsEntriesEscapingPermissionFilterViaEscapedQuote() {
    var request =
        request(
            Map.of(
                "criteria",
                List.of(),
                "_domains",
                List.of(
                    Map.of("domain", "self.firstName != \"abc\\\""),
                    Map.of("domain", "'\")) OR ((1=1 /*' = 'a' /* */"))));
    assertThrows(IllegalArgumentException.class, request::getCriteria);
  }

  @Test
  void rejectsDomainsEntriesEscapingPermissionFilterViaJavaDoubleQuotedString() {
    var request =
        request(
            Map.of(
                "criteria",
                List.of(),
                "_domains",
                List.of(
                    Map.of("domain", "self.firstName != j\"\""),
                    Map.of("domain", "\")) OR ((1=1 /* '\" = 'a' /* */"))));
    assertThrows(IllegalArgumentException.class, request::getCriteria);
  }

  @Test
  @Transactional
  void keepsPermissionFilterWithValidDomain() {
    var request = request(Map.of("_domain", "(self.firstName IS NOT NULL OR self.lastName = ')')"));
    assertEquals(1, request.getCriteria().createQuery(Contact.class, permission).count());
  }
}
