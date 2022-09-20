/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.meta.loader;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.common.XMLUtils;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.MetaViewCustom;
import com.axelor.meta.db.repo.MetaActionRepository;
import com.axelor.meta.db.repo.MetaModelRepository;
import com.axelor.meta.db.repo.MetaViewCustomRepository;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.views.AbstractView;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLViews {

  private static final Logger log = LoggerFactory.getLogger(XMLViews.class);

  private static final String LOCAL_SCHEMA = "object-views.xsd";
  private static final String REMOTE_SCHEMA = "object-views_" + ObjectViews.VERSION + ".xsd";

  private static final Set<String> VIEW_TYPES = new HashSet<>();

  private static final String INDENT_STRING = "  ";
  private static final String[] INDENT_PROPERTIES = {
    "eclipselink.indent-string",
    "com.sun.xml.internal.bind.indentString",
    "com.sun.xml.bind.indentString"
  };

  private static Marshaller marshaller;
  private static Unmarshaller unmarshaller;

  private static final Object DOCUMENT_BUILDER_FACTORY_MONITOR = new Object();
  private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY =
      XMLUtils.createDocumentBuilderFactory(true);

  private static final Supplier<Boolean> customizationEnabled =
      Suppliers.memoize(
          () -> AppSettings.get().getBoolean(AvailableAppSettings.VIEW_CUSTOMIZATION, true));

  static {
    try {
      init();
    } catch (JAXBException | SAXException e) {
      throw new RuntimeException(e);
    }
  }

  private XMLViews() {}

  private static void init() throws JAXBException, SAXException {
    if (unmarshaller != null) {
      return;
    }

    JAXBContext context = JAXBContext.newInstance(ObjectViews.class);
    unmarshaller = context.createUnmarshaller();
    marshaller = context.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    marshaller.setProperty(
        Marshaller.JAXB_SCHEMA_LOCATION,
        ObjectViews.NAMESPACE + " " + ObjectViews.getSecureNamespace() + "/" + REMOTE_SCHEMA);

    for (String name : INDENT_PROPERTIES) {
      try {
        marshaller.setProperty(name, INDENT_STRING);
        break;
      } catch (Exception e) {
        log.info("JAXB marshaller doesn't support property: {}", name);
      }
    }

    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = schemaFactory.newSchema(Resources.getResource(LOCAL_SCHEMA));

    unmarshaller.setSchema(schema);
    marshaller.setSchema(schema);

    // find supported views
    JsonSubTypes types = AbstractView.class.getAnnotation(JsonSubTypes.class);
    for (JsonSubTypes.Type type : types.value()) {
      JsonTypeName name = type.value().getAnnotation(JsonTypeName.class);
      if (name != null) {
        VIEW_TYPES.add(name.value());
      }
    }
  }

  public static ObjectViews unmarshal(InputStream stream) throws JAXBException {
    synchronized (unmarshaller) {
      return (ObjectViews) unmarshaller.unmarshal(stream);
    }
  }

  public static ObjectViews unmarshal(String xml) throws JAXBException {
    Reader reader = new StringReader(prepareXML(xml));

    synchronized (unmarshaller) {
      return (ObjectViews) unmarshaller.unmarshal(reader);
    }
  }

  public static ObjectViews unmarshal(Node node) throws JAXBException {
    JAXBElement<ObjectViews> element;

    synchronized (unmarshaller) {
      element = unmarshaller.unmarshal(node, ObjectViews.class);
    }

    return element.getValue();
  }

  public static void marshal(ObjectViews views, Writer writer) throws JAXBException {
    synchronized (marshaller) {
      marshaller.marshal(views, writer);
    }
  }

  public static Document parseXml(String xml)
      throws ParserConfigurationException, SAXException, IOException {
    synchronized (DOCUMENT_BUILDER_FACTORY_MONITOR) {
      final DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
      final InputSource is = new InputSource(new StringReader(prepareXML(xml)));
      return documentBuilder.parse(is);
    }
  }

  public static boolean isViewType(String type) {
    return VIEW_TYPES.contains(type);
  }

  private static String prepareXML(String xml) {
    StringBuilder sb = new StringBuilder("<?xml version='1.0' encoding='UTF-8'?>\n");
    sb.append("<object-views")
        .append(" xmlns='")
        .append(ObjectViews.NAMESPACE)
        .append("'")
        .append(" xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'")
        .append(" xsi:schemaLocation='")
        .append(ObjectViews.NAMESPACE)
        .append(" ")
        .append(ObjectViews.getSecureNamespace() + "/" + REMOTE_SCHEMA)
        .append("'")
        .append(">\n")
        .append(xml)
        .append("\n</object-views>");
    return sb.toString();
  }

  private static String strip(String xml) {
    String[] lines = xml.split("\n");
    StringBuilder sb = new StringBuilder();
    for (int i = 2; i < lines.length - 1; i++) {
      sb.append(lines[i] + "\n");
    }
    sb.deleteCharAt(sb.length() - 1);
    return StringUtils.stripIndent(sb.toString());
  }

  @SuppressWarnings("all")
  public static String toXml(Object obj, boolean strip) {

    ObjectViews views = new ObjectViews();
    StringWriter writer = new StringWriter();

    if (obj instanceof Action) {
      views.setActions(ImmutableList.of((Action) obj));
    }
    if (obj instanceof AbstractView) {
      views.setViews(ImmutableList.of((AbstractView) obj));
    }
    if (obj instanceof List) {
      views.setViews((List) obj);
    }
    try {
      marshal(views, writer);
    } catch (JAXBException e) {
      log.error(e.getMessage(), e);
    }
    String text = writer.toString();
    if (strip) {
      text = strip(text);
    }
    return text;
  }

  public static ObjectViews fromXML(String xml) throws JAXBException {
    if (Strings.isNullOrEmpty(xml)) return null;

    if (!xml.trim().startsWith("<?xml")) xml = prepareXML(xml);

    StringReader reader = new StringReader(xml);
    return (ObjectViews) unmarshaller.unmarshal(reader);
  }

  public static Map<String, Object> findViews(String model, Map<String, String> views) {
    final Map<String, Object> result = Maps.newHashMap();
    if (views == null || views.isEmpty()) {
      views = ImmutableMap.of("grid", "", "form", "");
    }
    for (Entry<String, String> entry : views.entrySet()) {
      final String type = entry.getKey();
      final String name = entry.getValue();
      final AbstractView view = findView(name, type, model);
      try {
        result.put(type, view);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    return result;
  }

  private static MetaViewCustom findCustomView(
      MetaViewCustomRepository views, String name, String type, String model) {
    User user = AuthUtils.getUser();
    List<String> conditions = new ArrayList<>();

    if (StringUtils.notBlank(name)) conditions.add("self.name = :name");
    if (StringUtils.notBlank(type)) conditions.add("self.type = :type");
    if (StringUtils.notBlank(model)) conditions.add("self.model = :model");

    // find personal
    String filter = String.join(" AND ", conditions) + " AND self.user = :user";

    MetaViewCustom custom =
        views
            .all()
            .filter(filter)
            .bind("name", name)
            .bind("type", type)
            .bind("model", model)
            .bind("user", user)
            .fetchOne();

    if (custom != null) {
      return custom;
    }

    // find shared
    filter = String.join(" AND ", conditions) + " AND self.shared = true";

    return views
        .all()
        .filter(filter)
        .bind("name", name)
        .bind("type", type)
        .bind("model", model)
        .fetchOne();
  }

  private static MetaView findMetaView(
      MetaViewRepository views, String name, String type, String model, String module, Long group) {
    final List<String> select = new ArrayList<>();
    if (name != null) {
      select.add("self.name = :name");
    }
    if (type != null) {
      select.add("self.type = :type");
    }
    if (model != null) {
      select.add("self.model = :model");
    }
    if (module != null) {
      select.add("self.module = :module");
    }
    if (group == null) {
      select.add("self.groups is empty");
    } else {
      select.add("self.groups[].id = :group");
    }
    select.add("(self.extension is null OR self.extension = false)");
    return views
        .all()
        .filter(Joiner.on(" AND ").join(select))
        .bind("name", name)
        .bind("type", type)
        .bind("model", model)
        .bind("module", module)
        .bind("group", group)
        .cacheable()
        .order("-priority")
        .fetchOne();
  }

  public static AbstractView findView(Long id) {
    final MetaView view = Beans.get(MetaViewRepository.class).find(id);
    if (view == null) {
      return null;
    }
    try {
      return unmarshal(view.getXml()).getViews().get(0);
    } catch (JAXBException e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  public static AbstractView findCustomView(Long id) {
    final MetaViewCustom view = Beans.get(MetaViewCustomRepository.class).find(id);
    if (view == null) {
      return null;
    }
    try {
      return unmarshal(view.getXml()).getViews().get(0);
    } catch (JAXBException e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  public static AbstractView findView(String name, String type) {
    return findView(name, type, null, null);
  }

  public static AbstractView findView(String name, String type, String model) {
    return findView(name, type, model, null);
  }

  public static boolean isCustomizationEnabled() {
    return customizationEnabled.get();
  }

  /**
   * Find view by the given parameters.
   *
   * <p>This method will find view in following order:
   *
   * <ol>
   *   <li>find custom view by name and current user
   *   <li>find view matching given params with user's group
   *   <li>find view matching given params but have no groups
   * </ol>
   *
   * @param name find by name
   * @param type find by type (name or model should be provided)
   * @param model find by model (name or type should be provided)
   * @param module (any of the other param should be provided)
   * @return
   */
  public static AbstractView findView(String name, String type, String model, String module) {

    final MetaViewRepository views = Beans.get(MetaViewRepository.class);
    final MetaViewCustomRepository customViews = Beans.get(MetaViewCustomRepository.class);

    final User user = AuthUtils.getUser();
    final Long group = user != null && user.getGroup() != null ? user.getGroup().getId() : null;

    MetaView view = null;
    MetaViewCustom custom = null;

    // find personalized view
    if (Boolean.TRUE.equals(isCustomizationEnabled()) && module == null && user != null) {
      custom = findCustomView(customViews, name, type, model);
    }

    // first find by name
    if (StringUtils.notBlank(name)) {
      // with group
      view = findMetaView(views, name, null, model, module, group);
      view = view == null ? findMetaView(views, name, null, null, module, group) : view;

      // without group
      view = view == null ? findMetaView(views, name, null, model, module, null) : view;
      view = view == null ? findMetaView(views, name, null, null, module, null) : view;

      if (view == null) {
        log.error("No such view found: {}", name);
        return null;
      }
    }

    // next find by type
    if (type != null && model != null) {
      view = view == null ? findMetaView(views, null, type, model, module, group) : view;
      view = view == null ? findMetaView(views, null, type, model, module, null) : view;
    }

    final AbstractView xmlView;
    final MetaModel metaModel;
    try {
      final String xml;

      if (custom == null) {
        if (view == null) {
          return null;
        }

        xml = view.getXml();
      } else {
        xml = custom.getXml();
      }

      final ObjectViews objectViews = unmarshal(xml);
      xmlView = objectViews.getViews().get(0);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return null;
    }
    if (view != null) {
      xmlView.setViewId(view.getId());
      xmlView.setHelpLink(view.getHelpLink());
      if (view.getModel() != null) {
        metaModel =
            Beans.get(MetaModelRepository.class)
                .all()
                .filter("self.fullName = :name")
                .bind("name", view.getModel())
                .cacheable()
                .autoFlush(false)
                .fetchOne();
        if (metaModel != null) {
          xmlView.setModelId(metaModel.getId());
        }
      }
    }
    if (custom != null) {
      xmlView.setCustomViewId(custom.getId());
      xmlView.setCustomViewShared(custom.getShared());
    }
    return xmlView;
  }

  public static Action findAction(String name) {
    final MetaAction metaAction = Beans.get(MetaActionRepository.class).findByName(name);
    final Action action;
    try {
      action = XMLViews.unmarshal(metaAction.getXml()).getActions().get(0);
      action.setActionId(metaAction.getId());
      return action;
    } catch (Exception e) {
      return null;
    }
  }
}
