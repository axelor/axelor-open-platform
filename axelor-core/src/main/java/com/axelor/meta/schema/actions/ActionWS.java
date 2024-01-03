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
package com.axelor.meta.schema.actions;

import com.axelor.common.ResourceUtils;
import com.axelor.common.StringUtils;
import com.axelor.common.XMLUtils;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.MetaStore;
import com.axelor.text.GroovyTemplates;
import com.axelor.text.StringTemplates;
import com.axelor.text.Templates;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@XmlType
public class ActionWS extends Action {

  private static final int DEFAULT_READ_TIMEOUT = 300;
  private static final int DEFAULT_CONNECT_TIMEOUT = 60;

  @XmlAttribute private String service;

  @XmlAttribute(name = "connect-timeout")
  private Integer connectTimeout;

  @XmlAttribute(name = "read-timeout")
  private Integer readTimeout;

  @XmlElement(name = "action")
  private List<WSAction> methods;

  public String getService() {
    return service;
  }

  public Integer getConnectTimeout() {
    if (connectTimeout == null) {
      return DEFAULT_CONNECT_TIMEOUT;
    }
    return connectTimeout;
  }

  public Integer getReadTimeout() {
    if (readTimeout == null) {
      return DEFAULT_READ_TIMEOUT;
    }
    return readTimeout;
  }

  public List<WSAction> getMethods() {
    return methods;
  }

  private ActionWS getRef() {
    if (service == null || !service.startsWith("ref:")) return null;
    String refName = service.replaceFirst("^ref:", "");
    Action ref = MetaStore.getAction(refName);
    if (ref == null || !(ref instanceof ActionWS))
      throw new IllegalArgumentException("No such web service: " + refName);
    if (((ActionWS) ref).getService().startsWith("ref:"))
      throw new IllegalArgumentException("Invalid web service: " + refName);
    return (ActionWS) ref;
  }

  private Object send(String location, WSAction act, String template, ActionHandler handler)
      throws IOException, ParserConfigurationException, SAXException, InterruptedException {

    InputStream templateStream = null;
    try {
      File templateFile = new File(template);
      if (templateFile.isFile()) {
        templateStream = new FileInputStream(templateFile);
      } else {
        templateStream = ResourceUtils.getResourceStream(template.replace("classpath:", ""));
      }

      if (templateStream == null) {
        throw new IllegalArgumentException();
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("No such template: " + template);
    }

    Templates engine = new StringTemplates('$', '$');
    if ("groovy".equals(act.engine)) {
      engine = new GroovyTemplates();
    }

    String payload = null;
    try (Reader reader = new InputStreamReader(templateStream)) {
      payload = handler.template(engine, reader);
    }
    Document envelope = XMLUtils.parse(new StringReader(payload));
    String namespace = envelope.getDocumentElement().getNamespaceURI();
    String charset = envelope.getXmlEncoding();

    if (StringUtils.isBlank(charset)) {
      charset = StandardCharsets.UTF_8.name();
    }

    String contentType = "text/xml";
    String actionHeader = "SOAPAction";

    // SOAP 1.2
    if (Objects.equals(namespace, "http://www.w3.org/2003/05/soap-envelope")) {
      contentType = "application/soap+xml";
      actionHeader = "action";
    }

    contentType = contentType + "; charset=" + charset;

    HttpClient httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(getConnectTimeout()))
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .proxy(ProxySelector.getDefault())
            .build();

    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(location))
            .timeout(Duration.ofSeconds(getReadTimeout()))
            .header("Content-Type", contentType)
            .header(actionHeader, act.getName())
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

    HttpResponse<InputStream> response =
        httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
    envelope = XMLUtils.parse(response.body());

    org.w3c.dom.Element body =
        XMLUtils.stream(envelope.getDocumentElement(), "Body").findFirst().orElse(null);

    if (body == null) {
      throw new IllegalStateException("No body found in response");
    }

    org.w3c.dom.Element result = XMLUtils.stream(body, "*").findFirst().orElse(null);

    if (result == null) {
      throw new IllegalStateException("No result found in response");
    }

    StringWriter writer = new StringWriter();
    try {
      XMLUtils.transform(result, writer, envelope.getXmlEncoding());
    } catch (TransformerException e) {
      throw new IllegalStateException(e);
    }

    return writer.toString();
  }

  private String getService(ActionWS ref, ActionHandler handler) {
    String url = ref == null ? service : ref.getService();
    Object service = handler.evaluate(url);

    if (service == null) {
      log.error("No such service: " + url);
      return null;
    }

    return service.toString();
  }

  @Override
  public Object evaluate(ActionHandler handler) {

    ActionWS ref = getRef();
    String url = getService(ref, handler);

    if (StringUtils.isBlank(url)) {
      return null;
    }

    if (ref != null) {
      ref.evaluate(handler);
    }

    List<Object> result = new ArrayList<>();
    log.info("action-ws (name): " + getName());
    for (WSAction act : methods) {
      Object template = handler.evaluate(act.template);
      if (template == null) {
        log.error("No such template: " + template);
        continue;
      }
      log.info("action-ws (method, template): " + act.getName() + ", " + template.toString());
      try {
        Object res = this.send(url, act, template.toString(), handler);
        result.add(res);
      } catch (Exception e) {
        log.error("error: " + e);
      }
    }
    return result;
  }

  @XmlType
  public static class WSAction extends Element {

    @XmlAttribute private String template;

    @XmlAttribute private String engine;

    public String getTemplate() {
      return template;
    }

    public String getEngine() {
      return engine;
    }
  }
}
