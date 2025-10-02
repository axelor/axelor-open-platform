/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.servlet;

import com.axelor.app.internal.AppFilter;
import com.axelor.i18n.I18n;
import com.axelor.i18n.I18nBundle;
import com.axelor.inject.Beans;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.zip.GZIPOutputStream;

@Singleton
public class I18nServlet extends HttpServlet {

  private static final long serialVersionUID = -6879530734799286544L;

  private static final String CONTENT_ENCODING = "Content-Encoding";
  private static final String ACCEPT_ENCODING = "Accept-Encoding";
  private static final String GZIP_ENCODING = "gzip";
  private static final String CONTENT_TYPE = "application/json; charset=utf8";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    Locale locale = AppFilter.getLocale();
    if (locale == null) {
      locale = req.getLocale();
    }

    final ResourceBundle bundle = I18n.getBundle(locale);
    if (bundle == null) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // Cache control headers
    String etag = '"' + I18nBundle.getHash(locale) + '"';
    resp.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=0, must-revalidate");
    resp.setHeader(HttpHeaders.ETAG, etag);

    // Check If-None-Match (ETag-based conditional request)
    String ifNoneMatch = req.getHeader(HttpHeaders.IF_NONE_MATCH);
    if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
      resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    resp.setContentType(CONTENT_TYPE);

    OutputStream out = resp.getOutputStream();
    if (req.getHeader(ACCEPT_ENCODING) != null
        && req.getHeader(ACCEPT_ENCODING).toLowerCase().contains(GZIP_ENCODING)) {
      resp.setHeader(CONTENT_ENCODING, GZIP_ENCODING);
      out = new GZIPOutputStream(out);
    }

    Enumeration<String> keys = bundle.getKeys();
    Map<String, String> messages = new HashMap<>();

    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      messages.put(key, bundle.getString(key));
    }

    try {
      final ObjectMapper mapper = Beans.get(ObjectMapper.class);
      final String json = mapper.writeValueAsString(messages);
      out.write(json.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } finally {
      out.close();
    }
  }
}
