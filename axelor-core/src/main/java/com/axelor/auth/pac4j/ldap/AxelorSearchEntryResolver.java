/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.ldap;

import org.ldaptive.SearchRequest;
import org.ldaptive.auth.AuthenticationCriteria;
import org.ldaptive.auth.SearchEntryResolver;

public class AxelorSearchEntryResolver extends SearchEntryResolver {

  @Override
  protected SearchRequest createSearchRequest(AuthenticationCriteria ac) {
    final SearchRequest request = super.createSearchRequest(ac);
    request.setBinaryAttributes(AxelorLdapProfileDefinition.PICTURE_JPEG);
    return request;
  }
}
