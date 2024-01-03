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
package com.axelor.auth.pac4j.local;

import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.extractor.FormExtractor;
import org.pac4j.core.util.Pac4jConstants;

public class JsonExtractor extends FormExtractor {

  public JsonExtractor() {
    super(Pac4jConstants.USERNAME, Pac4jConstants.PASSWORD);
  }

  @Override
  public Optional<Credentials> extract(WebContext context, SessionStore sessionStore) {
    return AuthPac4jInfo.isXHR(context)
        ? extractJson(context)
        : super.extract(context, sessionStore);
  }

  private Optional<Credentials> extractJson(WebContext context) {
    final Map<?, ?> data;
    try {
      data = new ObjectMapper().readValue(context.getRequestContent(), Map.class);
    } catch (IOException e) {
      return Optional.empty();
    }

    final String username = (String) data.get("username");
    final String password = (String) data.get("password");
    final String newPassword = (String) data.get("newPassword");
    if (username == null || password == null) {
      return Optional.empty();
    }

    return Optional.of(new AxelorFormCredentials(username, password, newPassword));
  }
}
