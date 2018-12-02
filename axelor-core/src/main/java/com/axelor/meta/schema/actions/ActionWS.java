/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.meta.schema.actions;

import com.axelor.meta.ActionHandler;
import com.axelor.meta.MetaStore;
import com.axelor.text.GroovyTemplates;
import com.axelor.text.StringTemplates;
import com.axelor.text.Templates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import groovy.util.slurpersupport.GPathResult;
import groovy.xml.XmlUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import wslite.soap.SOAPClient;
import wslite.soap.SOAPResponse;

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
      throws IOException, FileNotFoundException, ClassNotFoundException {

    File templateFile = new File(template);
    if (!templateFile.isFile()) {
      throw new IllegalArgumentException("No such template: " + template);
    }

    Templates engine = new StringTemplates('$', '$');
    if ("groovy".equals(act.engine)) {
      engine = new GroovyTemplates();
    }

    String payload = null;
    Reader reader = new FileReader(templateFile);
    try {
      payload = handler.template(engine, reader);
    } finally {
      reader.close();
    }
    Map<String, Object> params = Maps.newHashMap();

    params.put("connectTimeout", getConnectTimeout() * 1000);
    params.put("readTimeout", getReadTimeout() * 1000);

    SOAPClient client = new SOAPClient(location);
    SOAPResponse response = client.send(params, payload);

    GPathResult gpath = ((GPathResult) response.getBody()).children();
    String ns = gpath.lookupNamespace("");
    if (ns != null) {
      gpath.declareNamespace(ImmutableMap.of(":", ns));
    }

    return XmlUtil.serialize(gpath);
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

    if (Strings.isNullOrEmpty(url)) return null;

    if (ref != null) {
      ref.evaluate(handler);
    }

    List<Object> result = Lists.newArrayList();
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
