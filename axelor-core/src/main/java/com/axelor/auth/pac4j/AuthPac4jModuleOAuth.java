/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.auth.pac4j;

import com.axelor.common.StringUtils;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.servlet.ServletContext;
import org.pac4j.core.client.Client;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.GenericOAuth20Client;
import org.pac4j.oauth.client.GitHubClient;
import org.pac4j.oauth.client.Google2Client;
import org.pac4j.oauth.client.LinkedIn2Client;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.oauth.client.WechatClient;
import org.pac4j.oauth.client.WindowsLiveClient;
import org.pac4j.oauth.client.YahooClient;

public class AuthPac4jModuleOAuth extends AuthPac4jModuleForm {

  private static final String CONFIG_PREFIX = "auth.oauth.";
  private static Map<String, Map<String, String>> allSettings;

  private static final Map<
          String,
          Function<Map<String, String>, Client<? extends Credentials, ? extends CommonProfile>>>
      providers =
          ImmutableMap
              .<String,
                  Function<
                      Map<String, String>, Client<? extends Credentials, ? extends CommonProfile>>>
                  builder()
              .put("google", AuthPac4jModuleOAuth::setupGoogle)
              .put("facebook", AuthPac4jModuleOAuth::setupFacebook)
              .put("twitter", AuthPac4jModuleOAuth::setupTwitter)
              .put("yahoo", AuthPac4jModuleOAuth::setupYahoo)
              .put("linkedin", AuthPac4jModuleOAuth::setupLinkedIn)
              .put("windowslive", AuthPac4jModuleOAuth::setupWindowsLive)
              .put("wechat", AuthPac4jModuleOAuth::setupWeChat)
              .put("github", AuthPac4jModuleOAuth::setupGitHub)
              .build();

  public AuthPac4jModuleOAuth(ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected void configureClients() {
    addFormClientIfNotExclusive(allSettings);
    addCentralClients(allSettings, providers, AuthPac4jModuleOAuth::setupGeneric);
  }

  private static Client<? extends Credentials, ? extends CommonProfile> setupGeneric(
      Map<String, String> settings, String providerName) {
    final String key = settings.get("key");
    final String secret = settings.get("secret");
    final String authUrl = settings.get("auth.url");
    final String tokenUrl = settings.get("token.url");
    final String profileAttrsSetting = settings.get("profile.attrs");

    final String name = settings.getOrDefault("name", providerName);
    final String title = settings.getOrDefault("title", "OAuth 2.0");
    final String icon = settings.getOrDefault("icon", "img/signin/oauth2.png");

    final GenericOAuth20Client client = new GenericOAuth20Client();
    client.setName(name);
    client.setKey(key);
    client.setSecret(secret);

    if (StringUtils.notBlank(authUrl)) {
      client.setAuthUrl(authUrl);
    }
    if (StringUtils.notBlank(tokenUrl)) {
      client.setTokenUrl(tokenUrl);
    }

    if (StringUtils.notBlank(profileAttrsSetting)) {
      final Map<String, String> profileAttrsMap = new HashMap<>();
      for (final String item : profileAttrsSetting.split("\\s*,\\s*")) {
        final String[] entry = item.split("\\s*:\\s*");
        if (entry.length > 1) {
          final String attr = entry[0];
          final String value = entry[1];
          profileAttrsMap.put(attr, value);
        }
      }
      client.setProfileAttrs(profileAttrsMap);
    }

    setClientInfo(client.getName(), ImmutableMap.of("title", title, "icon", icon));
    return client;
  }

  private static Client<? extends Credentials, ? extends CommonProfile> setupGoogle(
      Map<String, String> settings) {
    final String key = settings.get("key");
    final String secret = settings.get("secret");
    final String title = settings.getOrDefault("title", "Google");
    final String icon = settings.getOrDefault("icon", "img/signin/google.svg");

    final Google2Client client = new Google2Client(key, secret);
    setClientInfo(client.getName(), ImmutableMap.of("title", title, "icon", icon));
    return client;
  }

  private static Client<? extends Credentials, ? extends CommonProfile> setupFacebook(
      Map<String, String> settings) {
    final String key = settings.get("key");
    final String secret = settings.get("secret");
    final String title = settings.getOrDefault("title", "Facebook");
    final String icon = settings.getOrDefault("icon", "img/signin/facebook.svg");

    final FacebookClient client = new FacebookClient(key, secret);
    setClientInfo(client.getName(), ImmutableMap.of("title", title, "icon", icon));
    return client;
  }

  private static Client<? extends Credentials, ? extends CommonProfile> setupTwitter(
      Map<String, String> settings) {
    final String key = settings.get("key");
    final String secret = settings.get("secret");
    final String title = settings.getOrDefault("title", "Twitter");
    final String icon = settings.getOrDefault("icon", "img/signin/twitter.svg");
    final boolean includeEmail = settings.getOrDefault("include.email", "true").equals("true");

    final TwitterClient client = new TwitterClient(key, secret, includeEmail);
    setClientInfo(client.getName(), ImmutableMap.of("title", title, "icon", icon));
    return client;
  }

  private static Client<? extends Credentials, ? extends CommonProfile> setupYahoo(
      Map<String, String> settings) {
    final String key = settings.get("key");
    final String secret = settings.get("secret");
    final String title = settings.getOrDefault("title", "Yahoo!");
    final String icon = settings.getOrDefault("icon", "img/signin/yahoo.svg");

    final YahooClient client = new YahooClient(key, secret);
    setClientInfo(client.getName(), ImmutableMap.of("title", title, "icon", icon));
    return client;
  }

  private static Client<? extends Credentials, ? extends CommonProfile> setupLinkedIn(
      Map<String, String> settings) {
    final String key = settings.get("key");
    final String secret = settings.get("secret");
    final String title = settings.getOrDefault("title", "LinkedIn");
    final String icon = settings.getOrDefault("icon", "img/signin/linkedin.svg");

    final LinkedIn2Client client = new LinkedIn2Client(key, secret);
    setClientInfo(client.getName(), ImmutableMap.of("title", title, "icon", icon));
    return client;
  }

  private static Client<? extends Credentials, ? extends CommonProfile> setupWindowsLive(
      Map<String, String> settings) {
    final String key = settings.get("key");
    final String secret = settings.get("secret");
    final String title = settings.getOrDefault("title", "Windows Live");
    final String icon = settings.getOrDefault("icon", "img/signin/microsoft.svg");

    final WindowsLiveClient client = new WindowsLiveClient(key, secret);
    setClientInfo(client.getName(), ImmutableMap.of("title", title, "icon", icon));
    return client;
  }

  private static Client<? extends Credentials, ? extends CommonProfile> setupWeChat(
      Map<String, String> settings) {
    final String key = settings.get("key");
    final String secret = settings.get("secret");
    final String title = settings.getOrDefault("title", "WeChat");
    final String icon = settings.getOrDefault("icon", "img/signin/wechat.svg");

    final WechatClient client = new WechatClient(key, secret);
    setClientInfo(client.getName(), ImmutableMap.of("title", title, "icon", icon));
    return client;
  }

  private static Client<? extends Credentials, ? extends CommonProfile> setupGitHub(
      Map<String, String> settings) {
    final String key = settings.get("key");
    final String secret = settings.get("secret");
    final String title = settings.getOrDefault("title", "GitHub");
    final String icon = settings.getOrDefault("icon", "img/signin/github.svg");

    final GitHubClient client = new GitHubClient(key, secret);
    setClientInfo(client.getName(), ImmutableMap.of("title", title, "icon", icon));
    return client;
  }

  public static boolean isEnabled() {
    if (allSettings == null) {
      allSettings = getAllSettings(CONFIG_PREFIX);
    }

    return !allSettings.isEmpty();
  }
}
