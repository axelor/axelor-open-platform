/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.extractor.FormExtractor;
import org.pac4j.core.util.Pac4jConstants;

public class JsonExtractor extends FormExtractor {

  public JsonExtractor() {
    super(Pac4jConstants.USERNAME, Pac4jConstants.PASSWORD);
  }

  @Override
  public Optional<Credentials> extract(CallContext ctx) {
    return AuthPac4jInfo.isXHR(ctx) ? extractJson(ctx.webContext()) : super.extract(ctx);
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
