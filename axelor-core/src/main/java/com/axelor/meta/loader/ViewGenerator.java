package com.axelor.meta.loader;

import com.axelor.app.AppConfig;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.repo.GroupRepository;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.common.XMLUtils;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.db.internal.DBHelper;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.Position;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.persist.Transactional;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.TypedQuery;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
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
import org.xml.sax.SAXException;

public class ViewGenerator {

  private static final Logger log = LoggerFactory.getLogger(ViewGenerator.class);

  private static final String STRING_DELIMITER = ",";
  private static final String TOOL_BAR = "toolbar";
  private static final String MENU_BAR = "menubar";
  private static final String PANEL_MAIL = "panel-mail";
  private static final Map<Position, Position> ROOT_NODE_POSITION_MAP =
      ImmutableMap.of(Position.AFTER, Position.INSIDE_LAST, Position.BEFORE, Position.INSIDE_FIRST);

  private static AppConfig appConfigProvider;
  @Inject private MetaViewRepository metaViewRepo;
  @Inject private GroupRepository groupRepo;

  static {
    final String appConfigProdiverName =
        AppSettings.get().get(AvailableAppSettings.APPLICATION_CONFIG_PROVIDER);

    if (StringUtils.notBlank(appConfigProdiverName)) {
      try {
        @SuppressWarnings("unchecked")
        final Class<AppConfig> cls = (Class<AppConfig>) Class.forName(appConfigProdiverName);
        appConfigProvider = Beans.get(cls);
      } catch (ClassNotFoundException e) {
        log.error(
            "Can't find class {} specified by {}",
            appConfigProdiverName,
            AvailableAppSettings.APPLICATION_CONFIG_PROVIDER);
      }
    }

    if (appConfigProvider == null) {
      appConfigProvider = featureName -> false;
    }
  }

  private static final XPathFactory XPATH_FACTORY = XMLUtils.createXPathFactory();
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
        public Iterator<String> getPrefixes(String namespaceURI) {
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

  @Transactional
  public boolean generate(MetaView view) {
    try {
      return generateChecked(view);
    } catch (XPathExpressionException
        | ParserConfigurationException
        | SAXException
        | IOException
        | JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  private TypedQuery<MetaView> findForCompute(Collection<String> names, boolean update) {
    final boolean namesEmpty = ObjectUtils.isEmpty(names);
    return JPA.em()
        .createQuery(
            "SELECT self FROM MetaView self LEFT JOIN self.groups viewGroup WHERE "
                + "((self.name IN :names OR :namesEmpty = TRUE) "
                + "AND (:update = TRUE OR NOT EXISTS ("
                + "SELECT computedView FROM MetaView computedView "
                + "WHERE computedView.name = self.name AND computedView.computed = TRUE))) "
                + "AND COALESCE(self.extension, FALSE) = FALSE "
                + "AND COALESCE(self.computed, FALSE) = FALSE "
                + "AND (self.name, self.priority, COALESCE(viewGroup.id, 0)) "
                + "IN (SELECT other.name, MAX(other.priority), COALESCE(otherGroup.id, 0) FROM MetaView other "
                + "LEFT JOIN other.groups otherGroup "
                + "WHERE COALESCE(other.extension, FALSE) = FALSE AND COALESCE(other.computed, FALSE) = FALSE "
                + "GROUP BY other.name, otherGroup) "
                + "AND EXISTS (SELECT extensionView FROM MetaView extensionView "
                + "WHERE extensionView.name = self.name AND extensionView.extension = TRUE) "
                + "GROUP BY self "
                + "ORDER BY self.id",
            MetaView.class)
        .setParameter("update", update)
        .setParameter("names", namesEmpty ? ImmutableSet.of("") : names)
        .setParameter("namesEmpty", namesEmpty);
  }

  @Transactional(rollbackOn = Exception.class)
  public boolean generateChecked(MetaView view)
      throws ParserConfigurationException, SAXException, IOException, XPathExpressionException,
          JAXBException {

    final MetaView originalView = getOriginalView(view);
    final List<MetaView> extensionViews = findExtensionMetaViewsByModuleOrder(originalView);
    final Query<MetaView> computedViewQuery =
        metaViewRepo
            .all()
            .filter("self.name = :name AND self.computed = TRUE")
            .bind("name", originalView.getName());

    if (extensionViews.isEmpty()) {
      computedViewQuery.remove();
      return false;
    }

    final String xml = originalView.getXml();
    final Document document = XMLViews.parseXml(xml);
    final Node viewNode = findViewNode(document);

    final MetaView computedView;
    final Iterator<MetaView> computedViewIt =
        computedViewQuery.order("xmlId").fetchStream().iterator();
    if (computedViewIt.hasNext()) {
      computedView = computedViewIt.next();
      while (computedViewIt.hasNext()) {
        metaViewRepo.remove(computedViewIt.next());
      }
    } else {
      final MetaView copy = metaViewRepo.copy(originalView, false);
      final String xmlId =
          MoreObjects.firstNonNull(originalView.getXmlId(), originalView.getName())
              + "__computed__";
      copy.setXmlId(xmlId);
      copy.setComputed(true);
      computedView = metaViewRepo.save(copy);
    }

    computedView.setPriority(originalView.getPriority() + 1);

    originalView.setDependentModules(null);
    originalView.setDependentFeatures(null);

    for (final MetaView extensionView : extensionViews) {
      final Document extensionDocument = XMLViews.parseXml(extensionView.getXml());
      final Node extensionViewNode = findViewNode(extensionDocument);

      for (final Node node : nodeListToList(extensionViewNode.getChildNodes())) {
        if (!(node instanceof Element)) {
          continue;
        }

        if ("extend".equals(node.getNodeName())) {
          processExtend(document, node, originalView, extensionView);
        } else {
          processAppend(document, node, viewNode, originalView);
        }
      }
    }

    final ObjectViews objectViews = XMLViews.unmarshal(document);
    final AbstractView finalView = objectViews.getViews().get(0);
    final String finalXml = XMLViews.toXml(finalView, true);
    computedView.setXml(finalXml);
    computedView.setModule(getLastModule(extensionViews));
    addGroups(computedView, finalView.getGroups());

    return true;
  }

  private void addGroups(MetaView view, String codes) {
    if (StringUtils.notBlank(codes)) {
      Arrays.stream(codes.split("\\s*,\\s*"))
          .forEach(
              code -> {
                Group group = groupRepo.findByCode(code);
                if (group == null) {
                  log.info("Creating a new user group: {}", code);
                  group = groupRepo.save(new Group(code, code));
                }
                view.addGroup(group);
              });
    }
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

  private List<MetaView> findExtensionMetaViewsByModuleOrder(MetaView view) {
    final List<MetaView> views = findExtensionMetaViews(view);
    final Map<String, List<MetaView>> viewsByModuleName =
        views.parallelStream()
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

  private List<MetaView> findExtensionMetaViews(MetaView view) {
    final List<String> select = new ArrayList<>();

    select.add("self.extension = true");
    select.add("self.name = :name");
    select.add("self.model = :model");
    select.add("self.type = :type");

    return metaViewRepo
        .all()
        .filter(Joiner.on(" AND ").join(select))
        .bind("name", view.getName())
        .bind("model", view.getModel())
        .bind("type", view.getType())
        .cacheable()
        .order("-priority")
        .order("id")
        .fetchStream()
        .filter(extView -> Objects.equals(extView.getGroups(), view.getGroups()))
        .collect(Collectors.toList());
  }

  private MetaView getOriginalView(MetaView view) {
    if (Boolean.TRUE.equals(view.getComputed())) {
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
        (Node) evaluateXPath(target, view.getName(), view.getType(), document, XPathConstants.NODE);

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

  private static void processAppend(
      Document document, Node extensionNode, Node viewNode, MetaView view)
      throws XPathExpressionException {
    final Node node = document.importNode(extensionNode, true);
    final Node panelMailNode =
        (Node)
            evaluateXPath(
                PANEL_MAIL, view.getName(), view.getType(), document, XPathConstants.NODE);
    if (panelMailNode == null) {
      viewNode.appendChild(node);
    } else {
      viewNode.insertBefore(node, panelMailNode);
    }
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

  private static Node doInsert(
      List<Element> elements, Position position, Node targetNode, Document document) {
    final Iterator<Element> it = elements.iterator();

    if (!it.hasNext()) {
      return targetNode;
    }

    Node node = doInsert(it.next(), position, targetNode, document);

    while (it.hasNext()) {
      node = doInsert(it.next(), Position.AFTER, node, document);
    }

    return node;
  }

  private static Node doInsert(
      Element element, Position position, Node targetNode, Document document) {
    final Node newChild = document.importNode(element, true);
    position.insert(targetNode, newChild);
    return newChild;
  }

  private static void doInsertToolBar(Element element, Document document, MetaView view)
      throws XPathExpressionException {
    final List<Element> elements;
    final Node targetNode;
    final Position position;
    final Node toolBarNode =
        (Node)
            evaluateXPath(TOOL_BAR, view.getName(), view.getType(), document, XPathConstants.NODE);

    if (toolBarNode != null) {
      elements = findElements(element.getChildNodes());
      targetNode = toolBarNode;
      position = Position.INSIDE_LAST;
    } else {
      elements = ImmutableList.of(element);
      targetNode =
          (Node) evaluateXPath("/", view.getName(), view.getType(), document, XPathConstants.NODE);
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
            evaluateXPath(MENU_BAR, view.getName(), view.getType(), document, XPathConstants.NODE);

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
    Node changedTargetNode = null;

    final List<Element> toolBarElements = filterElements(elements, TOOL_BAR);
    for (final Element element : toolBarElements) {
      changedTargetNode = doReplaceToolBar(element, document, view);
    }
    elements.removeAll(toolBarElements);

    final List<Element> menuBarElements = filterElements(elements, MENU_BAR);
    for (final Element element : menuBarElements) {
      changedTargetNode = doReplaceMenuBar(element, document, view);
    }
    elements.removeAll(menuBarElements);

    final List<Element> panelMailElements = filterElements(elements, PANEL_MAIL);
    for (final Element element : panelMailElements) {
      changedTargetNode = doReplacePanelMail(element, document, view);
    }
    elements.removeAll(panelMailElements);

    if (changedTargetNode != null) {
      doInsert(elements, Position.AFTER, changedTargetNode, document);
    } else {
      doReplace(elements, targetNode, document);
    }
  }

  @Nullable
  private static Node doReplace(List<Element> elements, Node targetNode, Document document) {
    if (elements.isEmpty()) {
      targetNode.getParentNode().removeChild(targetNode);
      return null;
    } else {
      final Node node = document.importNode(elements.get(0), true);
      targetNode.getParentNode().replaceChild(node, targetNode);
      return doInsert(elements.subList(1, elements.size()), Position.AFTER, node, document);
    }
  }

  private static Node doReplaceToolBar(Element element, Document document, MetaView view)
      throws XPathExpressionException {
    final List<Element> elements = ImmutableList.of(element);
    final Node toolBarNode =
        (Node)
            evaluateXPath(TOOL_BAR, view.getName(), view.getType(), document, XPathConstants.NODE);

    if (toolBarNode != null) {
      return doReplace(elements, toolBarNode, document);
    }

    final Node targetNode =
        (Node) evaluateXPath("/", view.getName(), view.getType(), document, XPathConstants.NODE);
    final Position position = Position.INSIDE_FIRST;
    return doInsert(elements, position, targetNode, document);
  }

  private static Node doReplaceMenuBar(Element element, Document document, MetaView view)
      throws XPathExpressionException {
    final List<Element> elements = ImmutableList.of(element);
    final Node menuBarNode =
        (Node)
            evaluateXPath(MENU_BAR, view.getName(), view.getType(), document, XPathConstants.NODE);

    if (menuBarNode != null) {
      return doReplace(elements, menuBarNode, document);
    }

    final Node targetNode;
    final Position position;
    final Node toolBarNode =
        (Node)
            evaluateXPath(TOOL_BAR, view.getName(), view.getType(), document, XPathConstants.NODE);

    if (toolBarNode != null) {
      targetNode = toolBarNode;
      position = Position.AFTER;
    } else {
      targetNode =
          (Node) evaluateXPath("/", view.getName(), view.getType(), document, XPathConstants.NODE);
      position = Position.INSIDE_FIRST;
    }

    return doInsert(elements, position, targetNode, document);
  }

  private static Node doReplacePanelMail(Element element, Document document, MetaView view)
      throws XPathExpressionException {
    final List<Element> elements = ImmutableList.of(element);
    final Node panelMailNode =
        (Node)
            evaluateXPath(
                PANEL_MAIL, view.getName(), view.getType(), document, XPathConstants.NODE);

    if (panelMailNode != null) {
      return doReplace(elements, panelMailNode, document);
    }

    final Node targetNode =
        (Node) evaluateXPath("/", view.getName(), view.getType(), document, XPathConstants.NODE);
    final Position position = Position.INSIDE_LAST;
    return doInsert(elements, position, targetNode, document);
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
        (Node) evaluateXPath(source, view.getName(), view.getType(), document, XPathConstants.NODE);

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
    return elements.stream()
        .filter(element -> nodeName.equals(element.getNodeName()))
        .collect(Collectors.toList());
  }

  @Transactional
  public long generate(Collection<String> names, boolean update) {
    final long count = generate(findForCompute(names, update));

    if (count == 0L && ObjectUtils.notEmpty(names)) {
      metaViewRepo
          .all()
          .filter("self.name IN :names AND self.computed = TRUE")
          .bind("names", names)
          .remove();
    }

    return count;
  }

  @Transactional
  public long generate(TypedQuery<MetaView> query) {
    query.setMaxResults(DBHelper.getJdbcFetchSize());
    return generate(query, 0, DBHelper.getJdbcFetchSize());
  }

  private long generate(TypedQuery<MetaView> query, int startOffset, int increment) {
    List<MetaView> views;
    int offset = startOffset;
    long count = 0;

    while (!(views = fetch(query, offset)).isEmpty()) {
      count += generate(views);
      offset += increment;
    }

    return count;
  }

  private List<MetaView> fetch(TypedQuery<MetaView> query, int offset) {
    query.setFirstResult(offset);
    return query.getResultList();
  }

  @Transactional
  public long generate(Query<MetaView> query) {
    return generate(query, 0, DBHelper.getJdbcFetchSize());
  }

  private long generate(Query<MetaView> query, int startOffset, int increment) {
    List<MetaView> views;
    int offset = startOffset;
    long count = 0;

    while (!(views = query.fetch(DBHelper.getJdbcFetchSize(), offset)).isEmpty()) {
      count += generate(views);
      offset += increment;
    }

    return count;
  }

  @Transactional
  public long generate(List<MetaView> views) {
    return views.stream().map(view -> generate(view) ? 1L : 0L).mapToLong(Long::longValue).sum();
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
    final String expr = subExpression.startsWith("/") ? subExpression.substring(1) : subExpression;
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
