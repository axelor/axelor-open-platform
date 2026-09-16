/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import java.util.Map;
import org.pac4j.core.client.Client;

/**
 * Human-readable names of the Axelor and pac4j authentication clients.
 *
 * <p>pac4j names a client after its class simple name unless configured otherwise, so clients are
 * matched by class: a renamed or subclassed client still gets the title of the first known class of
 * its hierarchy. Classes are matched by simple name, so that the optional pac4j modules are not
 * required.
 */
public final class ClientTitles {

  private static final Map<String, String> TITLES =
      Map.ofEntries(
          // Axelor
          Map.entry("AxelorFormClient", "Standard"),
          Map.entry("AxelorIndirectBasicAuthClient", "Basic authentication"),
          Map.entry("AxelorDirectBasicAuthClient", "Basic authentication"),
          Map.entry("AxelorApiKeyClient", "API key"),
          Map.entry("MfaClient", "Two-factor authentication"),
          // HTTP
          Map.entry("FormClient", "Standard"),
          Map.entry("IndirectBasicAuthClient", "Basic authentication"),
          Map.entry("DirectBasicAuthClient", "Basic authentication"),
          Map.entry("DirectDigestAuthClient", "Digest authentication"),
          Map.entry("HeaderClient", "HTTP header"),
          Map.entry("ParameterClient", "HTTP parameter"),
          Map.entry("CookieClient", "Cookie"),
          Map.entry("IpClient", "IP address"),
          Map.entry("DirectBearerAuthClient", "Bearer token"),
          Map.entry("X509Client", "X.509 certificate"),
          Map.entry("DirectX509Client", "X.509 certificate"),
          Map.entry("AnonymousClient", "Anonymous"),
          // OpenID Connect
          Map.entry("GoogleOidcClient", "Google"),
          Map.entry("AzureAd2Client", "Microsoft Entra ID"),
          Map.entry("AzureAdClient", "Microsoft Entra ID"),
          Map.entry("KeycloakOidcClient", "Keycloak"),
          Map.entry("AppleClient", "Apple"),
          Map.entry("OidcClient", "OpenID Connect"),
          // SAML
          Map.entry("SAML2Client", "SAML"),
          // CAS
          Map.entry("CasClient", "CAS"),
          Map.entry("DirectCasClient", "CAS"),
          Map.entry("DirectCasProxyClient", "CAS"),
          Map.entry("CasRestFormClient", "CAS"),
          Map.entry("CasRestBasicAuthClient", "CAS"),
          Map.entry("CasOAuthWrapperClient", "CAS"),
          // Kerberos
          Map.entry("IndirectKerberosClient", "Kerberos"),
          Map.entry("DirectKerberosClient", "Kerberos"),
          // OAuth
          Map.entry("BitbucketClient", "Bitbucket"),
          Map.entry("CronofyClient", "Cronofy"),
          Map.entry("DropBoxClient", "Dropbox"),
          Map.entry("FacebookClient", "Facebook"),
          Map.entry("FigShareClient", "figshare"),
          Map.entry("FoursquareClient", "Foursquare"),
          Map.entry("GitHubClient", "GitHub"),
          Map.entry("Google2Client", "Google"),
          Map.entry("HiOrgServerClient", "HiOrg-Server"),
          Map.entry("LinkedIn2Client", "LinkedIn"),
          Map.entry("OkClient", "OK"),
          Map.entry("OrcidClient", "ORCID"),
          Map.entry("PayPalClient", "PayPal"),
          Map.entry("QQClient", "QQ"),
          Map.entry("StravaClient", "Strava"),
          Map.entry("TwitterClient", "X (Twitter)"),
          Map.entry("VkClient", "VK"),
          Map.entry("WechatClient", "WeChat"),
          Map.entry("WeiboClient", "Weibo"),
          Map.entry("WindowsLiveClient", "Microsoft"),
          Map.entry("WordPressClient", "WordPress"),
          Map.entry("YahooClient", "Yahoo"),
          Map.entry("GenericOAuth20Client", "OAuth 2.0"),
          Map.entry("OAuth20Client", "OAuth 2.0"),
          Map.entry("OAuth10Client", "OAuth 1.0"));

  private ClientTitles() {}

  /**
   * Returns the title of the given client, from the first known class of its hierarchy.
   *
   * @param client the client
   * @return the title, or {@code null} if no class of the hierarchy is known
   */
  public static String of(Client client) {
    for (Class<?> klass = client.getClass(); klass != null; klass = klass.getSuperclass()) {
      final String title = TITLES.get(klass.getSimpleName());
      if (title != null) {
        return title;
      }
    }
    return null;
  }

  /**
   * Returns the title of the client with the given default name.
   *
   * @param clientName the client name
   * @return the title, or {@code null} if the name is unknown
   */
  public static String of(String clientName) {
    return TITLES.get(clientName);
  }
}
