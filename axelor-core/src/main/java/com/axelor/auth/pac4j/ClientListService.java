/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.pac4j.local.AxelorApiKeyClient;
import com.axelor.inject.Beans;
import com.google.inject.ImplementedBy;
import com.google.inject.Provider;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.http.ajax.AjaxRequestResolver;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
import org.pac4j.ldap.profile.service.LdapProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ImplementedBy(ClientListDefaultService.class)
public abstract class ClientListService implements Provider<List<Client>> {

  protected List<Client> clients = new ArrayList<>();

  protected String defaultClientName;

  private Set<String> indirectClientNames;

  private Set<String> directClientNames;

  protected static final AppSettings settings = AppSettings.get();

  protected static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected void init() {
    // API key auth
    clients.add(Beans.get(AxelorApiKeyClient.class));

    // Basic auth

    settings
        .getList(AvailableAppSettings.AUTH_LOCAL_BASIC_AUTH)
        .forEach(
            name -> {
              switch (name.toLowerCase()) {
                case "indirect":
                  clients.add(Beans.get(IndirectBasicAuthClient.class));
                  break;
                case "direct":
                  clients.add(Beans.get(DirectBasicAuthClient.class));
                  break;
                default:
                  throw new IllegalArgumentException("Invalid basic auth client: " + name);
              }
            });

    // Indirect and direct client names

    final Map<Boolean, List<Client>> grouped =
        clients.stream().collect(Collectors.groupingBy(IndirectClient.class::isInstance));

    final List<IndirectClient> indirectCients =
        grouped.getOrDefault(true, Collections.emptyList()).stream()
            .map(IndirectClient.class::cast)
            .collect(Collectors.toList());

    final AjaxRequestResolver ajaxRequestResolver = Beans.get(AjaxRequestResolver.class);
    indirectCients.forEach(client -> client.setAjaxRequestResolver(ajaxRequestResolver));

    indirectClientNames =
        Collections.unmodifiableSet(
            (Set<String>)
                indirectCients.stream()
                    .map(Client::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));

    if (!indirectClientNames.isEmpty()) {
      logger
          .atInfo()
          .setMessage("Indirect clients: {}")
          .addArgument(() -> indirectClientNames.stream().collect(Collectors.joining(", ")))
          .log();
    }

    directClientNames =
        Collections.unmodifiableSet(
            (Set<String>)
                grouped.getOrDefault(false, Collections.emptyList()).stream()
                    .map(Client::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));

    if (!directClientNames.isEmpty()) {
      logger
          .atInfo()
          .setMessage("Direct clients: {}")
          .addArgument(() -> directClientNames.stream().collect(Collectors.joining(", ")))
          .log();
    }

    // LDAP

    final String ldapServerUrl = settings.get(AvailableAppSettings.AUTH_LDAP_SERVER_URL, null);
    if (ldapServerUrl != null) {
      final AuthPac4jInfo authPac4jInfo = Beans.get(AuthPac4jInfo.class);
      authPac4jInfo.setAuthenticator(Beans.get(LdapProfileService.class));
      logger.info("LDAP: {}", ldapServerUrl);
    }
  }

  @Override
  public List<Client> get() {
    return clients;
  }

  public String getDefaultClientName() {
    return defaultClientName;
  }

  public Set<String> getIndirectClientNames() {
    return indirectClientNames;
  }

  public Set<String> getDirectClientNames() {
    return directClientNames;
  }
}
