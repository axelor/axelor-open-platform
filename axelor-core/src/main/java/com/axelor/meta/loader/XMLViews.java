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
package com.axelor.meta.loader;

import com.axelor.app.AppConfig;
import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.Query;
import com.axelor.db.internal.DBHelper;
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
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
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

      if (view == null) {
        throw new IllegalArgumentException("No such view found: " + name);
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
    return xmlView;
  }

  private static List<MetaView> findExtensionMetaViews(String name, String model, String type) {

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
        .bind("group", group)
        .cacheable()
        .order("-priority")
        .fetch();
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

  static class FinalViewGenerator {

    private static final int NUM_WORKERS = Runtime.getRuntime().availableProcessors();
    private static final int FETCH_INCREMENT = NUM_WORKERS * DBHelper.getJdbcFetchSize();
    private static final String STRING_DELIMITER = ",";
    private static final String TOOL_BAR = "toolbar";
    private static final String MENU_BAR = "menubar";
    private static final String PANEL_MAIL = "panel-mail";
    private static final Map<Position, Position> ROOT_NODE_POSITION_MAP =
        ImmutableMap.of(
            Position.AFTER, Position.INSIDE_LAST, Position.BEFORE, Position.INSIDE_FIRST);

    @Inject private MetaViewRepository metaViewRepo;

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

      final MetaView originalView = getOriginalView(view);
      final List<MetaView> extensionViews = findExtensionMetaViewsByModuleOrder(originalView);

      if (extensionViews.isEmpty()) {
        Optional.ofNullable(metaViewRepo.findByNameAndComputed(originalView.getName(), true))
            .ifPresent(metaViewRepo::remove);
        return;
      }

      final String xml = originalView.getXml();
      final Document document = parseXml(xml);
      final Node viewNode = findViewNode(document);

      final MetaView computedView =
          Optional.ofNullable(metaViewRepo.findByNameAndComputed(originalView.getName(), true))
              .orElseGet(
                  () -> {
                    final MetaView copy = metaViewRepo.copy(originalView, false);
                    copy.setComputed(true);
                    metaViewRepo.persist(copy);
                    return copy;
                  });
      computedView.setPriority(originalView.getPriority() + 1);

      originalView.setDependentModules(null);
      originalView.setDependentFeatures(null);

      for (final MetaView extensionView : extensionViews) {
        final Document extensionDocument = parseXml(extensionView.getXml());
        final Node extensionViewNode = findViewNode(extensionDocument);

        for (final Node node : nodeListToList(extensionViewNode.getChildNodes())) {
          if (!(node instanceof Element)) {
            continue;
          }

          if ("extend".equals(node.getNodeName())) {
            processExtend(document, node, originalView, extensionView);
          } else {
            processAppend(document, node, viewNode);
          }
        }
      }

      final ObjectViews objectViews = unmarshal(document);
      final String finalXml = toXml(objectViews.getViews().get(0), true);
      computedView.setXml(finalXml);
      computedView.setModule(getLastModule(extensionViews));
    }

    @Nullable
    private static String getLastModule(List<MetaView> metaViews) {
      for (final ListIterator<MetaView> it = metaViews.listIterator(metaViews.size());
          it.hasPrevious(); ) {
        final String module = it.previous().getModule();

        if (StringUtils.notBlank(module)) {
          return module;
        }
      }

      return null;
    }

    private static List<MetaView> findExtensionMetaViewsByModuleOrder(MetaView view) {
      final List<MetaView> views =
          findExtensionMetaViews(view.getName(), view.getModel(), view.getType());
      final Map<String, List<MetaView>> viewsByModuleName =
          views
              .parallelStream()
              .collect(Collectors.groupingBy(v -> Optional.ofNullable(v.getModule()).orElse("")));
      final List<MetaView> result = new ArrayList<>(views.size());

      // Add views by module resolution order.
      for (final String moduleName : ModuleManager.getResolution()) {
        result.addAll(viewsByModuleName.getOrDefault(moduleName, Collections.emptyList()));
        viewsByModuleName.remove(moduleName);
      }

      // Add remaining views not found in module resolution.
      for (final List<MetaView> metaViews : viewsByModuleName.values()) {
        result.addAll(metaViews);
      }

      return result;
    }

    private MetaView getOriginalView(MetaView view) {
      if (view.getComputed()) {
        log.warn("View is computed: {}", view.getName());
        return Optional.ofNullable(metaViewRepo.findByNameAndComputed(view.getName(), false))
            .orElseThrow(NoSuchElementException::new);
      }

      return view;
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

      Optional.ofNullable(ModuleManager.getModule(extensionView.getModule()))
          .map(Module::isRemovable)
          .filter(removable -> removable)
          .ifPresent(
              removable -> {
                addDependentModule(view, extensionView.getModule());
              });

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

      for (final Element extendItemElement : findElements(extensionNode.getChildNodes())) {
        switch (extendItemElement.getNodeName()) {
          case "insert":
            doInsert(extendItemElement, targetNode, document, view);
            break;
          case "replace":
            doReplace(extendItemElement, targetNode, document, view);
            break;
          case "move":
            doMove(extendItemElement, targetNode, document, view, extensionView);
            break;
          case "attribute":
            doAttribute(extendItemElement, targetNode);
            break;
          default:
            log.error(
                "View {}(id={}): unknown extension tag: {}",
                extensionView.getName(),
                extensionView.getXmlId(),
                extendItemElement.getNodeName());
        }
      }
    }

    private static void processAppend(Document document, Node extensionNode, Node viewNode) {
      final Node node = document.importNode(extensionNode, true);
      viewNode.appendChild(node);
    }

    private static void doInsert(
        Node extendItemNode, Node targetNode, Document document, MetaView view)
        throws XPathExpressionException {
      final List<Element> elements = findElements(extendItemNode.getChildNodes());

      final List<Element> toolBarElements = filterElements(elements, TOOL_BAR);
      for (final Element element : toolBarElements) {
        doInsertToolBar(element, document, view);
      }
      elements.removeAll(toolBarElements);

      final List<Element> menuBarElements = filterElements(elements, MENU_BAR);
      for (final Element element : menuBarElements) {
        doInsertMenuBar(element, document, view);
      }
      elements.removeAll(menuBarElements);

      final List<Element> panelMailElements = filterElements(elements, PANEL_MAIL);
      for (final Element element : panelMailElements) {
        doInsertPanelMail(element, document, view);
      }
      elements.removeAll(panelMailElements);

      final NamedNodeMap attributes = extendItemNode.getAttributes();
      final String positionValue = getNodeAttributeValue(attributes, "position");
      Position position = Position.get(positionValue);

      if (isRootNode(targetNode)) {
        switch (position) {
          case BEFORE:
          case INSIDE_FIRST:
            final Node menuBarNode =
                (Node)
                    evaluateXPath(
                        MENU_BAR, view.getName(), view.getType(), document, XPathConstants.NODE);

            if (menuBarNode != null) {
              targetNode = menuBarNode;
              position = Position.AFTER;
            } else {
              final Node toolBarNode =
                  (Node)
                      evaluateXPath(
                          TOOL_BAR, view.getName(), view.getType(), document, XPathConstants.NODE);
              if (toolBarNode != null) {
                targetNode = toolBarNode;
                position = Position.AFTER;
              } else {
                position = Position.INSIDE_FIRST;
              }
            }

            break;
          case AFTER:
          case INSIDE_LAST:
            final Node panelMailNode =
                (Node)
                    evaluateXPath(
                        PANEL_MAIL, view.getName(), view.getType(), document, XPathConstants.NODE);

            if (panelMailNode != null) {
              targetNode = panelMailNode;
              position = Position.BEFORE;
            } else {
              position = Position.INSIDE_LAST;
            }

            break;
          default:
            throw new IllegalArgumentException(position.toString());
        }
      }

      doInsert(elements, position, targetNode, document);
    }

    private static void doInsert(
        List<Element> elements, Position position, Node targetNode, Document document) {
      Node currentNode = targetNode;

      for (final Element element : elements) {
        final Node node = document.importNode(element, true);
        position.insert(currentNode, node);
        currentNode = node;
      }
    }

    private static void doInsertToolBar(Element element, Document document, MetaView view)
        throws XPathExpressionException {
      final List<Element> elements;
      final Node targetNode;
      final Position position;
      final Node toolBarNode =
          (Node)
              evaluateXPath(
                  TOOL_BAR, view.getName(), view.getType(), document, XPathConstants.NODE);

      if (toolBarNode != null) {
        elements = findElements(element.getChildNodes());
        targetNode = toolBarNode;
        position = Position.INSIDE_LAST;
      } else {
        elements = ImmutableList.of(element);
        targetNode =
            (Node)
                evaluateXPath("/", view.getName(), view.getType(), document, XPathConstants.NODE);
        position = Position.INSIDE_FIRST;
      }

      doInsert(elements, position, targetNode, document);
    }

    private static void doInsertMenuBar(Element element, Document document, MetaView view)
        throws XPathExpressionException {
      final List<Element> elements;
      final Node targetNode;
      final Position position;
      final Node menuBarNode =
          (Node)
              evaluateXPath(
                  MENU_BAR, view.getName(), view.getType(), document, XPathConstants.NODE);

      if (menuBarNode != null) {
        elements = findElements(element.getChildNodes());
        targetNode = menuBarNode;
        position = Position.INSIDE_LAST;
      } else {
        elements = ImmutableList.of(element);
        final Node toolBarNode =
            (Node)
                evaluateXPath(
                    TOOL_BAR, view.getName(), view.getType(), document, XPathConstants.NODE);

        if (toolBarNode != null) {
          targetNode = toolBarNode;
          position = Position.AFTER;
        } else {
          targetNode =
              (Node)
                  evaluateXPath("/", view.getName(), view.getType(), document, XPathConstants.NODE);
          position = Position.INSIDE_FIRST;
        }
      }

      doInsert(elements, position, targetNode, document);
    }

    private static void doInsertPanelMail(Element element, Document document, MetaView view)
        throws XPathExpressionException {
      final List<Element> elements = ImmutableList.of(element);
      final Node targetNode;
      final Position position;
      final Node panelMailNode =
          (Node)
              evaluateXPath(
                  PANEL_MAIL, view.getName(), view.getType(), document, XPathConstants.NODE);

      if (panelMailNode != null) {
        doReplace(elements, panelMailNode, document);
        return;
      }

      targetNode =
          (Node) evaluateXPath("/", view.getName(), view.getType(), document, XPathConstants.NODE);
      position = Position.INSIDE_LAST;
      doInsert(elements, position, targetNode, document);
    }

    private static void doReplace(
        Node extendItemNode, Node targetNode, Document document, MetaView view)
        throws XPathExpressionException {
      final List<Element> elements = findElements(extendItemNode.getChildNodes());

      final List<Element> toolBarElements = filterElements(elements, TOOL_BAR);
      for (final Element element : toolBarElements) {
        doReplaceToolBar(element, document, view);
      }
      elements.removeAll(toolBarElements);

      final List<Element> menuBarElements = filterElements(elements, MENU_BAR);
      for (final Element element : menuBarElements) {
        doReplaceMenuBar(element, document, view);
      }
      elements.removeAll(menuBarElements);

      final List<Element> panelMailElements = filterElements(elements, PANEL_MAIL);
      for (final Element element : panelMailElements) {
        doReplacePanelMail(element, document, view);
      }
      elements.removeAll(panelMailElements);

      doReplace(elements, targetNode, document);
    }

    private static void doReplace(List<Element> elements, Node targetNode, Document document) {
      if (elements.isEmpty()) {
        targetNode.getParentNode().removeChild(targetNode);
      } else {
        final Node node = document.importNode(elements.get(0), true);
        targetNode.getParentNode().replaceChild(node, targetNode);
        doInsert(elements.subList(1, elements.size()), Position.AFTER, node, document);
      }
    }

    private static void doReplaceToolBar(Element element, Document document, MetaView view)
        throws XPathExpressionException {
      final List<Element> elements = ImmutableList.of(element);
      final Node toolBarNode =
          (Node)
              evaluateXPath(
                  TOOL_BAR, view.getName(), view.getType(), document, XPathConstants.NODE);

      if (toolBarNode != null) {
        doReplace(elements, toolBarNode, document);
        return;
      }

      final Node targetNode =
          (Node) evaluateXPath("/", view.getName(), view.getType(), document, XPathConstants.NODE);
      final Position position = Position.INSIDE_FIRST;
      doInsert(elements, position, targetNode, document);
    }

    private static void doReplaceMenuBar(Element element, Document document, MetaView view)
        throws XPathExpressionException {
      final List<Element> elements = ImmutableList.of(element);
      final Node menuBarNode =
          (Node)
              evaluateXPath(
                  MENU_BAR, view.getName(), view.getType(), document, XPathConstants.NODE);

      if (menuBarNode != null) {
        doReplace(elements, menuBarNode, document);
        return;
      }

      final Node targetNode;
      final Position position;
      final Node toolBarNode =
          (Node)
              evaluateXPath(
                  TOOL_BAR, view.getName(), view.getType(), document, XPathConstants.NODE);

      if (toolBarNode != null) {
        targetNode = toolBarNode;
        position = Position.AFTER;
      } else {
        targetNode =
            (Node)
                evaluateXPath("/", view.getName(), view.getType(), document, XPathConstants.NODE);
        position = Position.INSIDE_FIRST;
      }

      doInsert(elements, position, targetNode, document);
    }

    private static void doReplacePanelMail(Element element, Document document, MetaView view)
        throws XPathExpressionException {
      final List<Element> elements = ImmutableList.of(element);
      final Node panelMailNode =
          (Node)
              evaluateXPath(
                  PANEL_MAIL, view.getName(), view.getType(), document, XPathConstants.NODE);

      if (panelMailNode != null) {
        doReplace(elements, panelMailNode, document);
        return;
      }

      final Node targetNode =
          (Node) evaluateXPath("/", view.getName(), view.getType(), document, XPathConstants.NODE);
      final Position position = Position.INSIDE_LAST;
      doInsert(elements, position, targetNode, document);
    }

    private static void doMove(
        Node extendItemNode,
        Node targetNode,
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
      Position position = Position.get(positionValue);

      if (isRootNode(targetNode)) {
        position = ROOT_NODE_POSITION_MAP.getOrDefault(position, Position.INSIDE_LAST);
      }

      position.insert(targetNode, sourceNode);
    }

    private static boolean isRootNode(Node node) {
      return Optional.ofNullable(node)
          .map(Node::getParentNode)
          .map(Node::getNodeName)
          .orElse("")
          .equals(ObjectViews.class.getAnnotation(XmlRootElement.class).name());
    }

    private static void doAttribute(Node extendItemNode, Node targetNode) {
      final NamedNodeMap attributes = extendItemNode.getAttributes();
      final String name = getNodeAttributeValue(attributes, "name");
      final String value = getNodeAttributeValue(attributes, "value");

      if (!(targetNode instanceof Element)) {
        log.error("Can change attributes only on elements: {}", targetNode);
        return;
      }

      final Element targetElement = ((Element) targetNode);

      if (StringUtils.isEmpty(value)) {
        targetElement.removeAttribute(name);
      } else {
        targetElement.setAttribute(name, value);
      }
    }

    private static List<Element> findElements(NodeList nodeList) {
      return nodeListToStream(nodeList)
          .filter(node -> node instanceof Element)
          .map(node -> (Element) node)
          .collect(Collectors.toList());
    }

    private static List<Element> filterElements(List<Element> elements, String nodeName) {
      return elements
          .stream()
          .filter(element -> nodeName.equals(element.getNodeName()))
          .collect(Collectors.toList());
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
      final String expr =
          subExpression.startsWith("/") ? subExpression.substring(1) : subExpression;
      return String.format(expr.isEmpty() ? rootExpr : rootExpr + "/" + expr, type, name, expr);
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
