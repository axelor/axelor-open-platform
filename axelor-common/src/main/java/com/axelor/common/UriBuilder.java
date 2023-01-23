/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Build and manipulate URLs */
public class UriBuilder {

  private String scheme;
  private String userInfo;
  private String host;
  private String port;
  private PathBuilder pathBuilder = new PathBuilder();
  private final Map<String, String> queryParams = new LinkedHashMap<>();
  private String fragment;

  private static final String SCHEME_PATTERN = "([^:/?#]+):";
  private static final String USERINFO_PATTERN = "([^@\\[/?#]*)";
  private static final String HOST_PATTERN = "([^\\[/?#:]*)";
  private static final String PORT_PATTERN = "([^/?#]*)";
  private static final String PATH_PATTERN = "([^?#]*)";
  private static final String QUERY_PATTERN = "([^#]*)";
  private static final String FRAGMENT_PATTERN = "(.*)";
  private static final Pattern URI_PATTERN =
      Pattern.compile(
          "^("
              + SCHEME_PATTERN
              + ")?"
              + "(//("
              + USERINFO_PATTERN
              + "@)?"
              + HOST_PATTERN
              + "(:"
              + PORT_PATTERN
              + ")?"
              + ")?"
              + PATH_PATTERN
              + "(\\?"
              + QUERY_PATTERN
              + ")?"
              + "(#"
              + FRAGMENT_PATTERN
              + ")?");

  private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

  private UriBuilder() {}

  /**
   * Create an empty builder instance
   *
   * @return the uri builder
   */
  public static UriBuilder empty() {
    return new UriBuilder();
  }

  /**
   * Create builder from the given parameters
   *
   * @param scheme the scheme
   * @param userInfo the user info
   * @param host the host
   * @param port the port
   * @param path the path
   * @param queryParams the query params
   * @param fragment the fragment
   * @return the uri builder
   */
  public static UriBuilder of(
      String scheme,
      String userInfo,
      String host,
      String port,
      String path,
      Map<String, String> queryParams,
      String fragment) {
    return new UriBuilder()
        .setScheme(scheme)
        .setUserInfo(userInfo)
        .setHost(host)
        .setPort(port)
        .addPath(path)
        .addQueryParams(queryParams)
        .setFragment(fragment);
  }

  /**
   * Create builder from the given parameters
   *
   * @param scheme the scheme
   * @param userInfo the user info
   * @param host the host
   * @param port the port
   * @param path the path
   * @param queryParams the query params
   * @param fragment the fragment
   * @return the uri builder
   */
  public static UriBuilder of(
      String scheme,
      String userInfo,
      String host,
      String port,
      String path,
      String queryParams,
      String fragment) {
    return new UriBuilder()
        .setScheme(scheme)
        .setUserInfo(userInfo)
        .setHost(host)
        .setPort(port)
        .addPath(path)
        .addQueryParams(queryParams)
        .setFragment(fragment);
  }

  /**
   * Create builder from the given uri
   *
   * @param uri the uri
   * @return the uri builder
   */
  public static UriBuilder from(String uri) {
    Matcher matcher = URI_PATTERN.matcher(uri);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Not a valid url : " + uri);
    }

    UriBuilder builder = new UriBuilder();
    String scheme = matcher.group(2);
    String userInfo = matcher.group(5);
    String host = matcher.group(6);
    String port = matcher.group(8);
    String path = matcher.group(9);
    String query = matcher.group(11);
    String fragment = matcher.group(13);

    builder.setScheme(scheme);
    builder.setUserInfo(userInfo);
    builder.setHost(host);
    builder.setPort(port);
    builder.addPath(path);
    builder.addQueryParams(query);
    builder.setFragment(fragment);

    return builder;
  }

  /**
   * Set the fragment
   *
   * @param fragment the fragment
   * @return the uri builder
   */
  public UriBuilder setFragment(String fragment) {
    if (StringUtils.notBlank(fragment) && fragment.startsWith("#")) {
      fragment = fragment.substring(1);
    }
    this.fragment = fragment;
    return this;
  }

  /**
   * Parse and add the given query into query parameters.
   *
   * <p>Parameters are separated with {@code '&'} and their values, if any, with {@code '='}.
   *
   * @param query the query
   * @return the uri builder
   */
  public UriBuilder addQueryParams(String query) {
    if (StringUtils.notBlank(query)) {
      Matcher matcher = QUERY_PARAM_PATTERN.matcher(query);
      while (matcher.find()) {
        String name = matcher.group(1);
        String eq = matcher.group(2);
        String value = matcher.group(3);
        addQueryParam(name, (value != null ? value : (StringUtils.notBlank(eq) ? "" : null)));
      }
    } else {
      this.queryParams.clear();
    }
    return this;
  }

  /**
   * Clear existing query parameters and add multiple query parameters and values
   *
   * @param queryParams the params
   * @return the uri builder
   */
  public UriBuilder addQueryParams(Map<String, String> queryParams) {
    if (ObjectUtils.notEmpty(queryParams)) {
      for (Map.Entry<String, String> entry : queryParams.entrySet()) {
        addQueryParam(entry.getKey(), entry.getValue());
      }
    }
    return this;
  }

  /**
   * Add multiple query parameters and values
   *
   * @param queryParams the params
   * @return the uri builder
   */
  public UriBuilder setQueryParams(Map<String, String> queryParams) {
    this.queryParams.clear();
    if (ObjectUtils.notEmpty(queryParams)) {
      for (Map.Entry<String, String> entry : queryParams.entrySet()) {
        addQueryParam(entry.getKey(), entry.getValue());
      }
    }
    return this;
  }

  /**
   * Add the given name and value as query parameters.
   *
   * @param name the name
   * @param value the value
   * @return the uri builder
   */
  public UriBuilder addQueryParam(String name, String value) {
    this.queryParams.put(name, value);
    return this;
  }

  /**
   * Add the path to the previous path if existing.
   *
   * @param path the path
   * @return the uri builder
   */
  public UriBuilder addPath(String path) {
    this.pathBuilder.append(path);
    return this;
  }

  /**
   * Set the port
   *
   * @param port the port
   * @return the uri builder
   */
  public UriBuilder setPort(String port) {
    this.port = port;
    return this;
  }

  /**
   * Set the host
   *
   * @param host the host
   * @return the uri builder
   */
  public UriBuilder setHost(String host) {
    this.host = host;
    return this;
  }

  /**
   * Set the user info
   *
   * @param userInfo the user info
   * @return the uri builder
   */
  public UriBuilder setUserInfo(String userInfo) {
    this.userInfo = userInfo;
    return this;
  }

  /**
   * Set the scheme
   *
   * @param scheme the scheme
   * @return the uri builder
   */
  public UriBuilder setScheme(String scheme) {
    this.scheme = scheme;
    return this;
  }

  /**
   * Create a {@link URI} from this instance
   *
   * @return the uri
   */
  public URI toUri() {
    try {
      return new URI(
          this.scheme,
          this.userInfo,
          this.host,
          getPort(),
          pathBuilder.getPath(),
          getQuery(),
          this.fragment);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Unable to create URI : " + e.getMessage(), e);
    }
  }

  private int getPort() {
    if (StringUtils.isBlank(this.port)) {
      return -1;
    }
    return Integer.parseInt(this.port);
  }

  private String getQuery() {
    if (ObjectUtils.isEmpty(this.queryParams)) {
      return null;
    }
    StringBuilder queryBuilder = new StringBuilder();
    this.queryParams.forEach(
        (name, value) -> {
          if (queryBuilder.length() != 0) {
            queryBuilder.append('&');
          }
          queryBuilder.append(name);
          if (value != null) {
            queryBuilder.append('=').append(value);
          }
        });
    return queryBuilder.toString();
  }

  static class PathBuilder {

    private final List<String> paths = new ArrayList<>();

    public void append(String path) {
      this.paths.add(path);
    }

    public String getPath() {
      StringJoiner joiner = new StringJoiner("", "/", "/");
      for (String path : this.paths) {
        joiner.add(path);
      }
      return getSanitizedPath(joiner.toString());
    }

    private static String getSanitizedPath(String path) {
      int index = path.indexOf("//");
      if (index >= 0) {
        StringBuilder sanitized = new StringBuilder(path);
        while (index != -1) {
          sanitized.deleteCharAt(index);
          index = sanitized.indexOf("//", index);
        }
        path = sanitized.toString();
      }
      return removeTrailingSlash(path);
    }

    private static String removeTrailingSlash(String path) {
      if (path.endsWith("/")) {
        path = path.substring(0, path.length() - 1);
      }
      return path;
    }
  }
}
