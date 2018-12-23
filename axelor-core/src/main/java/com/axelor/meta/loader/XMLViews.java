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
package com.axelor.meta.loader;

import com.axelor.app.AppConfig;
import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.Query;
import com.axelor.db.internal.DBHelper;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaScanner;
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
import com.axelor.meta.schema.views.Position;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.inject.persist.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
  private static DocumentBuilderFactory documentBuilderFactory;

  private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();
  private static final NamespaceContext NS_CONTEXT =
      new NamespaceContext() {
        @Override
        public String getNamespaceURI(String prefix) {
          return ObjectViews.NAMESPACE;
        }

        @Override
        public String getPrefix(String namespaceURI) {
          throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Object> getPrefixes(String namespaceURI) {
          throw new UnsupportedOperationException();
        }
      };

  private static final Pattern NS_PATTERN = Pattern.compile("/(\\w)");

  private static final LoadingCache<String, XPathExpression> XPATH_EXPRESSION_CACHE =
      CacheBuilder.newBuilder()
          .maximumSize(10_000)
          .build(
              new CacheLoader<String, XPathExpression>() {
                public XPathExpression load(String key) throws Exception {
                  XPath xPath;

                  synchronized (XPATH_FACTORY) {
                    xPath = XPATH_FACTORY.newXPath();
                  }

                  xPath.setNamespaceContext(NS_CONTEXT);
                  return xPath.compile(NS_PATTERN.matcher(key).replaceAll("/:$1"));
                }
              });

  private static final String APP_CONFIG_PROVIDER_KEY = "application.config.provider";
  private static AppConfig appConfigProvider;

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
        ObjectViews.NAMESPACE + " " + ObjectViews.NAMESPACE + "/" + REMOTE_SCHEMA);

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

    documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    // This adds default attributes to generated XML
    // documentBuilderFactory.setSchema(schema);

    final String appConfigProdiverName = AppSettings.get().get(APP_CONFIG_PROVIDER_KEY);

    if (StringUtils.notBlank(appConfigProdiverName)) {
      try {
        @SuppressWarnings("unchecked")
        final Class<AppConfig> cls = (Class<AppConfig>) Class.forName(appConfigProdiverName);
        appConfigProvider = Beans.get(cls);
      } catch (ClassNotFoundException e) {
        log.error(
            "Can't find class {} specified by {}", appConfigProdiverName, APP_CONFIG_PROVIDER_KEY);
      }
    }

    if (appConfigProvider == null) {
      appConfigProvider = featureName -> false;
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
    final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    final InputSource is = new InputSource(new StringReader(prepareXML(xml)));
    return documentBuilder.parse(is);
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
        .append(ObjectViews.NAMESPACE + "/" + REMOTE_SCHEMA)
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

  /** Apply pending updates if auto-update watch is running. */
  public static void applyHotUpdates() {
    ViewWatcher.process();
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

  public static AbstractView findView(String name, String type) {
    return findView(name, type, null, null);
  }

  public static AbstractView findView(String name, String type, String model) {
    return findView(name, type, model, null);
  }

  /**
   * Find view by the given parameters.
   *
   * <p>This method will find view in following order:
   *
   * <ol>
   *   <li>find custom view by name & current user
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
    if (module == null && name != null && user != null) {
      custom = customViews.findByUser(name, model, user);
      custom = custom == null ? customViews.findByUser(name, user) : custom;
    }

    // make sure hot updates are applied
    applyHotUpdates();

    // first find by name
    if (name != null) {
      // with group
      view = findMetaView(views, name, null, model, module, group);
      view = view == null ? findMetaView(views, name, null, null, module, group) : view;

      // without group
      view = view == null ? findMetaView(views, name, null, model, module, null) : view;
      view = view == null ? findMetaView(views, name, null, null, module, null) : view;
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
    return xmlView;
  }

  private static List<MetaView> findExtensionMetaViews(
      String name, String model, String type, String module) {

    final MetaViewRepository repo = Beans.get(MetaViewRepository.class);
    final User user = AuthUtils.getUser();
    final Long group = user != null && user.getGroup() != null ? user.getGroup().getId() : null;
    final List<String> select = new ArrayList<>();

    select.add("self.extension = true");
    select.add("self.name = :name");
    select.add("self.model = :model");
    select.add("self.type = :type");

    if (group == null) {
      select.add("self.groups is empty");
    } else {
      select.add("(self.groups is empty OR self.groups[].id = :group)");
    }

    return repo.all()
        .filter(Joiner.on(" AND ").join(select))
        .bind("name", name)
        .bind("model", model)
        .bind("type", type)
        .bind("module", module)
        .bind("group", group)
        .cacheable()
        .order("-priority")
        .fetch();
  }

  /**
   * Find view extensions.
   *
   * @param name name of the form extension
   * @param model the form extension model
   * @param type extension view type
   * @param module the module
   * @return list of the form extensions
   */
  public static List<AbstractView> findExtensions(
      String name, String model, String type, String module) {
    final List<MetaView> metaViews = findExtensionMetaViews(name, model, type, module);
    final List<AbstractView> all = new ArrayList<>();
    for (MetaView view : metaViews) {
      try {
        final AbstractView xmlView = XMLViews.unmarshal(view.getXml()).getViews().get(0);
        all.add(xmlView);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    return all;
  }

  public static Action findAction(String name) {
    applyHotUpdates();
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

  static class FinalXmlGenerator {

    private static final int NUM_WORKERS = Runtime.getRuntime().availableProcessors();
    private static final int FETCH_INCREMENT = NUM_WORKERS * DBHelper.getJdbcFetchSize();
    private static final String STRING_DELIMITER = ",";

    public void generate(MetaView view) {
      try {
        generateChecked(view);
      } catch (XPathExpressionException
          | ParserConfigurationException
          | SAXException
          | IOException
          | JAXBException e) {
        throw new RuntimeException(e);
      }
    }

    @Transactional(rollbackOn = Exception.class)
    public void generateChecked(MetaView view)
        throws ParserConfigurationException, SAXException, IOException, XPathExpressionException,
            JAXBException {

      final List<MetaView> extensionViews =
          findExtensionMetaViews(view.getName(), view.getModel(), view.getType(), view.getModule());

      if (extensionViews.isEmpty()) {
        return;
      }

      final String xml = view.getComputed() ? getOriginalXml(view) : view.getXml();
      final Document document = parseXml(xml);
      final Node viewNode = findViewNode(document);

      view.setDependentModules(null);
      view.setDependentFeatures(null);

      for (final MetaView extensionView : extensionViews) {
        final Document extensionDocument = parseXml(extensionView.getXml());
        final Node extensionViewNode = findViewNode(extensionDocument);

        for (final Node node : nodeListToList(extensionViewNode.getChildNodes())) {
          if (!(node instanceof Element)) {
            continue;
          }

          if ("extend".equals(node.getNodeName())) {
            processExtend(document, node, view, extensionView);
          } else {
            processAppend(document, node, viewNode);
          }
        }
      }

      final ObjectViews objectViews = unmarshal(document);
      final String finalXml = toXml(objectViews.getViews().get(0), true);
      view.setXml(finalXml);
      view.setComputed(true);
    }

    private static String getOriginalXml(MetaView view) throws IOException, JAXBException {
      if (StringUtils.isBlank(view.getSourceFile())) {
        throw new UnsupportedOperationException(
            String.format("Extending default views is not supported: %s", view.getName()));
      }

      final List<URL> urls = MetaScanner.findAll(view.getModule(), "views", view.getSourceFile());

      if (urls.size() != 1) {
        throw new IllegalStateException(
            String.format(
                "Expected only one source file in %s for %s",
                view.getModule(), view.getSourceFile()));
      }

      final URL url = urls.iterator().next();
      final ObjectViews objectViews;

      try (final InputStream stream = url.openStream()) {
        objectViews = unmarshal(stream);
      }

      final AbstractView sourceView =
          Optional.ofNullable(objectViews.getViews())
              .orElse(Collections.emptyList())
              .stream()
              .filter(abstractView -> Objects.equals(view.getName(), abstractView.getName()))
              .filter(abstractView -> Objects.equals(view.getXmlId(), abstractView.getXmlId()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new NoSuchElementException(
                          String.format(
                              "View not found in %s: %s(id=%s)",
                              view.getModule(), view.getName(), view.getXmlId())));

      return toXml(sourceView, true);
    }

    private static Node findViewNode(Document document) {
      return nodeListToStream(document.getFirstChild().getChildNodes())
          .filter(node -> node instanceof Element)
          .findFirst()
          .orElseThrow(NoSuchElementException::new);
    }

    private void processExtend(
        Document document, Node extensionNode, MetaView view, MetaView extensionView)
        throws XPathExpressionException {

      final NamedNodeMap extendAttributes = extensionNode.getAttributes();
      final String feature = getNodeAttributeValue(extendAttributes, "if-feature");

      if (StringUtils.notBlank(feature)) {
        addDependentFeature(view, feature);

        if (!appConfigProvider.hasFeature(feature)) {
          return;
        }
      }

      final String module = getNodeAttributeValue(extendAttributes, "if-module");

      if (StringUtils.notBlank(module)) {
        addDependentModule(view, module);

        if (!ModuleManager.isInstalled(module)) {
          return;
        }
      }

      final String target = getNodeAttributeValue(extendAttributes, "target");
      final Node targetNode =
          (Node)
              evaluateXPath(target, view.getName(), view.getType(), document, XPathConstants.NODE);

      if (targetNode == null) {
        log.error(
            "View {}(id={}): extend target not found: {}",
            extensionView.getName(),
            extensionView.getXmlId(),
            target);
        return;
      }

      if (!(targetNode instanceof Element)) {
        log.error(
            "View {}(id={}): node is not an element: {}",
            extensionView.getName(),
            extensionView.getXmlId(),
            target);
        return;
      }

      final Element targetElement = (Element) targetNode;
      final List<Node> extendItemNodes =
          nodeListToStream(extensionNode.getChildNodes())
              .filter(extendItemNode -> extendItemNode instanceof Element)
              .collect(Collectors.toList());

      for (final Node extendItemNode : extendItemNodes) {
        switch (extendItemNode.getNodeName()) {
          case "insert":
            doInsert(extendItemNode, targetElement, document);
            break;
          case "replace":
            doReplace(extendItemNode, targetElement, document);
            break;
          case "move":
            doMove(extendItemNode, targetElement, document, view, extensionView);
            break;
          case "attribute":
            doAttribute(extendItemNode, targetElement);
            break;
          default:
            log.error(
                "View {}(id={}): unknown extension tag: {}",
                extensionView.getName(),
                extensionView.getXmlId(),
                extendItemNode.getNodeName());
        }
      }
    }

    private static void processAppend(Document document, Node extensionNode, Node viewNode) {
      final Node node = document.importNode(extensionNode, true);
      viewNode.appendChild(node);
    }

    private static void doInsert(Node extendItemNode, Element targetElement, Document document) {
      final NamedNodeMap attributes = extendItemNode.getAttributes();
      final String positionValue = getNodeAttributeValue(attributes, "position");
      final Position position = Position.get(positionValue);
      final Node refNode = position.getRefNodeFunc().apply(targetElement);
      final Node parentNode = refNode != null ? refNode.getParentNode() : targetElement;

      nodeListToStream(extendItemNode.getChildNodes())
          .filter(node -> node instanceof Element)
          .map(node -> document.importNode(node, true))
          .forEach(node -> parentNode.insertBefore(node, refNode));
    }

    private static void doReplace(Node extendItemNode, Element targetElement, Document document) {
      final Node refNode = Position.AFTER.getRefNodeFunc().apply(targetElement);
      final Node parentNode = refNode != null ? refNode.getParentNode() : targetElement;

      nodeListToStream(extendItemNode.getChildNodes())
          .filter(node -> node instanceof Element)
          .map(node -> document.importNode(node, true))
          .forEach(node -> parentNode.insertBefore(node, refNode));
      parentNode.removeChild(targetElement);
    }

    private static void doMove(
        Node extendItemNode,
        Element targetElement,
        Document document,
        MetaView view,
        MetaView extensionView)
        throws XPathExpressionException {
      final NamedNodeMap attributes = extendItemNode.getAttributes();
      final String source = getNodeAttributeValue(attributes, "source");
      final Node sourceNode =
          (Node)
              evaluateXPath(source, view.getName(), view.getType(), document, XPathConstants.NODE);

      if (sourceNode == null) {
        log.error(
            "View {}(id={}): move source not found: {}",
            extensionView.getName(),
            extensionView.getXmlId(),
            sourceNode);
        return;
      }

      final String positionValue = getNodeAttributeValue(attributes, "position");
      final Position position = Position.get(positionValue);
      final Node refNode = position.getRefNodeFunc().apply(targetElement);
      final Node parentNode = refNode != null ? refNode.getParentNode() : targetElement;

      parentNode.insertBefore(sourceNode, refNode);
    }

    private static void doAttribute(Node extendItemNode, Element targetElement) {
      final NamedNodeMap attributes = extendItemNode.getAttributes();
      final String name = getNodeAttributeValue(attributes, "name");
      final String value = getNodeAttributeValue(attributes, "value");
      targetElement.setAttribute(name, value);
    }

    public void parallelGenerate(Query<MetaView> query) {
      final ExecutorService pool = Executors.newFixedThreadPool(NUM_WORKERS);

      for (int i = 0; i < NUM_WORKERS; ++i) {
        final int startOffset = i * DBHelper.getJdbcFetchSize();
        pool.execute(() -> generate(query, startOffset, FETCH_INCREMENT));
      }

      shutdownAndAwaitTermination(pool);
    }

    public void generate(Query<MetaView> query) {
      generate(query, 0, DBHelper.getJdbcFetchSize());
    }

    private void generate(Query<MetaView> query, int startOffset, int increment) {
      List<MetaView> views;
      int offset = startOffset;

      while (!(views = query.fetch(DBHelper.getJdbcFetchSize(), offset)).isEmpty()) {
        generate(views);
        offset += increment;
      }
    }

    @Transactional
    public void generate(List<MetaView> views) {
      views.forEach(this::generate);
    }

    private static void shutdownAndAwaitTermination(ExecutorService pool) {
      pool.shutdown();

      try {
        while (!pool.awaitTermination(1, TimeUnit.HOURS)) {
          log.warn("Pool {} takes too long to terminate.", pool);
        }
      } catch (InterruptedException e) {
        pool.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    private static String getNodeAttributeValue(NamedNodeMap attributes, String name) {
      final Node item = attributes.getNamedItem(name);
      return item != null ? item.getNodeValue() : "";
    }

    private static Object evaluateXPath(
        String subExpression, String name, String type, Object item, QName returnType)
        throws XPathExpressionException {
      return evaluateXPath(prepareXPathExpression(subExpression, name, type), item, returnType);
    }

    private static Object evaluateXPath(String expression, Object item, QName returnType)
        throws XPathExpressionException {
      XPathExpression xPathExpression = XPATH_EXPRESSION_CACHE.getUnchecked(expression);

      synchronized (xPathExpression) {
        return xPathExpression.evaluate(item, returnType);
      }
    }

    private static String prepareXPathExpression(String subExpression, String name, String type) {
      final String rootExpr = "/:object-views/:%s[@name='%s']";
      return String.format(
          subExpression.isEmpty() ? rootExpr : rootExpr + "/" + subExpression,
          type,
          name,
          subExpression);
    }

    private static Stream<Node> nodeListToStream(NodeList nodeList) {
      return nodeListToList(nodeList).stream();
    }

    private static List<Node> nodeListToList(NodeList nodeList) {
      return new AbstractList<Node>() {
        @Override
        public int size() {
          return nodeList.getLength();
        }

        @Override
        public Node get(int index) {
          return Optional.ofNullable(nodeList.item(index))
              .orElseThrow(IndexOutOfBoundsException::new);
        }
      };
    }

    private static void addDependentFeature(MetaView view, String featureName) {
      final Set<String> dependentFeatures = stringToSet(view.getDependentFeatures());
      dependentFeatures.add(featureName);
      view.setDependentFeatures(iterableToString(dependentFeatures));
    }

    private static void addDependentModule(MetaView view, String moduleName) {
      final Set<String> dependentModules = stringToSet(view.getDependentModules());
      dependentModules.add(moduleName);
      view.setDependentModules(iterableToString(dependentModules));
    }

    private static Set<String> stringToSet(String text) {
      return StringUtils.isBlank(text)
          ? Sets.newHashSet()
          : Sets.newHashSet(text.split(STRING_DELIMITER));
    }

    private static String iterableToString(Iterable<? extends CharSequence> elements) {
      return String.join(STRING_DELIMITER, elements);
    }
  }
}
