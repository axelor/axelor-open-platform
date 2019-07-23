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

import com.axelor.app.AppSettings;
import javax.servlet.ServletContext;
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

  public static final String CONFIG_OAUTH_GENERIC_KEY = "auth.oauth.generic.key";
  public static final String CONFIG_OAUTH_GENERIC_SECRET = "auth.oauth.generic.secret";
  public static final String CONFIG_OAUTH_GENERIC_AUTH_URL = "auth.oauth.generic.auth.url";
  public static final String CONFIG_OAUTH_GENERIC_TOKEN_URL = "auth.oauth.generic.token.url";

  public static final String CONFIG_OAUTH_GOOGLE_KEY = "auth.oauth.google.key";
  public static final String CONFIG_OAUTH_GOOGLE_SECRET = "auth.oauth.google.secret";

  public static final String CONFIG_OAUTH_FACEBOOK_KEY = "auth.oauth.facebook.key";
  public static final String CONFIG_OAUTH_FACEBOOK_SECRET = "auth.oauth.facebook.secret";

  public static final String CONFIG_OAUTH_TWITTER_KEY = "auth.oauth.twitter.key";
  public static final String CONFIG_OAUTH_TWITTER_SECRET = "auth.oauth.twitter.secret";

  public static final String CONFIG_OAUTH_YAHOO_KEY = "auth.oauth.yahoo.key";
  public static final String CONFIG_OAUTH_YAHOO_SECRET = "auth.oauth.yahoo.secret";

  public static final String CONFIG_OAUTH_LINKEDIN_KEY = "auth.oauth.linkedin.key";
  public static final String CONFIG_OAUTH_LINKEDIN_SECRET = "auth.oauth.linkedin.secret";

  public static final String CONFIG_OAUTH_WINDOWSLIVE_KEY = "auth.oauth.windowslive.key";
  public static final String CONFIG_OAUTH_WINDOWSLIVE_SECRET = "auth.oauth.windowslive.secret";

  public static final String CONFIG_OAUTH_WECHAT_KEY = "auth.oauth.wechat.key";
  public static final String CONFIG_OAUTH_WECHAT_SECRET = "auth.oauth.wechat.secret";

  public static final String CONFIG_OAUTH_GITHUB_KEY = "auth.oauth.github.key";
  public static final String CONFIG_OAUTH_GITHUB_SECRET = "auth.oauth.github.secret";

  public AuthPac4jModuleOAuth(ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected void configureCentralClients() {
    final AppSettings settings = AppSettings.get();

    final String genericKey = settings.get(CONFIG_OAUTH_GENERIC_KEY, null);
    if (genericKey != null) {
      final String genericSecret = settings.get(CONFIG_OAUTH_GENERIC_SECRET, null);
      final String genericAuthUrl = settings.get(CONFIG_OAUTH_GENERIC_AUTH_URL, null);
      final String genericTokenUrl = settings.get(CONFIG_OAUTH_GENERIC_TOKEN_URL, null);
      final GenericOAuth20Client client = new GenericOAuth20Client();

      client.setKey(genericKey);
      client.setSecret(genericSecret);

      if (genericAuthUrl != null) {
        client.setAuthUrl(genericAuthUrl);
      }

      if (genericTokenUrl != null) {
        client.setTokenUrl(genericTokenUrl);
      }

      addClient(client);
    }

    final String googleKey = settings.get(CONFIG_OAUTH_GOOGLE_KEY, null);
    if (googleKey != null) {
      final String googleSecret = settings.get(CONFIG_OAUTH_GOOGLE_SECRET, null);
      Google2Client client = new Google2Client(googleKey, googleSecret);
      addClient(client);
    }

    final String facebookKey = settings.get(CONFIG_OAUTH_FACEBOOK_KEY, null);
    if (facebookKey != null) {
      final String facebookSecret = settings.get(CONFIG_OAUTH_FACEBOOK_SECRET, null);
      FacebookClient client = new FacebookClient(facebookKey, facebookSecret);
      addClient(client);
    }

    final String twitterKey = settings.get(CONFIG_OAUTH_TWITTER_KEY, null);
    if (twitterKey != null) {
      final String twitterSecret = settings.get(CONFIG_OAUTH_TWITTER_SECRET, null);
      TwitterClient client = new TwitterClient(twitterKey, twitterSecret, true);
      addClient(client);
    }

    final String yahooKey = settings.get(CONFIG_OAUTH_YAHOO_KEY, null);
    if (yahooKey != null) {
      final String yahooSecret = settings.get(CONFIG_OAUTH_YAHOO_SECRET, null);
      YahooClient client = new YahooClient(yahooKey, yahooSecret);
      addClient(client);
    }

    final String linkedInKey = settings.get(CONFIG_OAUTH_LINKEDIN_KEY, null);
    if (linkedInKey != null) {
      final String linkedInSecret = settings.get(CONFIG_OAUTH_LINKEDIN_SECRET, null);
      LinkedIn2Client client = new LinkedIn2Client(linkedInKey, linkedInSecret);
      addClient(client);
    }

    final String windowsLiveKey = settings.get(CONFIG_OAUTH_WINDOWSLIVE_KEY, null);
    if (windowsLiveKey != null) {
      final String windowsLiveSecret = settings.get(CONFIG_OAUTH_WINDOWSLIVE_SECRET, null);
      WindowsLiveClient client = new WindowsLiveClient(windowsLiveKey, windowsLiveSecret);
      addClient(client);
    }

    final String weChatKey = settings.get(CONFIG_OAUTH_WECHAT_KEY, null);
    if (weChatKey != null) {
      final String weChatSecret = settings.get(CONFIG_OAUTH_WECHAT_SECRET, null);
      WechatClient client = new WechatClient(weChatKey, weChatSecret);
      addClient(client);
    }

    final String gitHubKey = settings.get(CONFIG_OAUTH_GITHUB_KEY, null);
    if (gitHubKey != null) {
      final String gitHubSecret = settings.get(CONFIG_OAUTH_GITHUB_SECRET, null);
      GitHubClient client = new GitHubClient(gitHubKey, gitHubSecret);
      addClient(client);
    }
  }

  public static boolean isEnabled() {
    final AppSettings settings = AppSettings.get();
    return settings.get(CONFIG_OAUTH_GENERIC_KEY, null) != null
        || settings.get(CONFIG_OAUTH_GOOGLE_KEY, null) != null
        || settings.get(CONFIG_OAUTH_FACEBOOK_KEY, null) != null
        || settings.get(CONFIG_OAUTH_TWITTER_KEY, null) != null
        || settings.get(CONFIG_OAUTH_YAHOO_KEY, null) != null
        || settings.get(CONFIG_OAUTH_LINKEDIN_KEY, null) != null
        || settings.get(CONFIG_OAUTH_WINDOWSLIVE_KEY, null) != null
        || settings.get(CONFIG_OAUTH_WECHAT_KEY, null) != null
        || settings.get(CONFIG_OAUTH_GITHUB_KEY, null) != null;
  }
}
