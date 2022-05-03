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

import com.axelor.common.StringUtils;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.views.Position;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/** Compute xml of computed views from base view and their extensions. */
public class ComputedViewXmlProcessor extends AbstractXmlProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(ComputedViewXmlProcessor.class);

  private final MetaView baseView;
  private final List<MetaView> extendsViews;

  private Document baseDocument;
  private Node baseNode;

  private final Set<String> dependentModules = new HashSet<>();
  private final Set<String> dependentFeatures = new HashSet<>();

  public ComputedViewXmlProcessor(MetaView baseView, List<MetaView> extendsViews) {
    this.baseView = baseView;
    this.extendsViews = extendsViews;
  }

  public Document compute()
      throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
    baseDocument = XMLViews.parseXml(baseView.getXml());
    baseNode = findViewNode(baseDocument);

    for (final MetaView extensionView : extendsViews) {
      processExtension(extensionView);
    }

    return baseDocument;
  }

  private void processExtension(MetaView extensionView)
      throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
    final Document extensionDocument = XMLViews.parseXml(extensionView.getXml());
    final Node extensionViewNode = findViewNode(extensionDocument);

    for (final Node node : nodeListToList(extensionViewNode.getChildNodes())) {
      if (!(node instanceof Element)) {
        continue;
      }

      if ("extend".equals(node.getNodeName())) {
        processExtend(node, extensionView);
      } else {
        processAppend(node);
      }
    }
  }

  private void processExtend(Node extensionNode, MetaView extensionView)
      throws XPathExpressionException {

    final NamedNodeMap extendAttributes = extensionNode.getAttributes();
    final String feature = getNodeAttributeValue(extendAttributes, "if-feature");

    if (StringUtils.notBlank(feature)) {
      addDependentFeature(feature);

      if (!appConfigProvider.hasFeature(feature)) {
        return;
      }
    }

    final String module = getNodeAttributeValue(extendAttributes, "if-module");

    if (StringUtils.notBlank(module)) {
      addDependentModule(module);

      if (!ModuleManager.isInstalled(module)) {
        return;
      }
    }

    final String target = getNodeAttributeValue(extendAttributes, "target");
    final Node targetNode =
        (Node)
            evaluateXPath(
                target, baseView.getName(), baseView.getType(), baseDocument, XPathConstants.NODE);

    if (targetNode == null) {
      LOG.error(
          "View {}(id={}): extend target not found: {}",
          extensionView.getName(),
          extensionView.getXmlId(),
          target);
      return;
    }

    for (final Element extendItemElement : findElements(extensionNode.getChildNodes())) {
      switch (extendItemElement.getNodeName()) {
        case "insert":
          doInsert(extendItemElement, targetNode);
          break;
        case "replace":
          doReplace(extendItemElement, targetNode);
          break;
        case "move":
          doMove(extendItemElement, targetNode, extensionView);
          break;
        case "attribute":
          doAttribute(extendItemElement, targetNode);
          break;
        default:
          LOG.error(
              "View {}(id={}): unknown extension tag: {}",
              extensionView.getName(),
              extensionView.getXmlId(),
              extendItemElement.getNodeName());
      }
    }
  }

  private void processAppend(Node extensionNode) throws XPathExpressionException {
    final Node node = baseDocument.importNode(extensionNode, true);
    final Node panelMailNode =
        (Node)
            evaluateXPath(
                PANEL_MAIL,
                baseView.getName(),
                baseView.getType(),
                baseDocument,
                XPathConstants.NODE);
    if (panelMailNode == null) {
      baseNode.appendChild(node);
    } else {
      baseNode.insertBefore(node, panelMailNode);
    }
  }

  private void doInsert(Node extendItemNode, Node targetNode) throws XPathExpressionException {
    final List<Element> elements = findElements(extendItemNode.getChildNodes());

    final List<Element> toolBarElements = filterElements(elements, TOOL_BAR);
    for (final Element element : toolBarElements) {
      doInsertToolBar(element);
    }
    elements.removeAll(toolBarElements);

    final List<Element> menuBarElements = filterElements(elements, MENU_BAR);
    for (final Element element : menuBarElements) {
      doInsertMenuBar(element);
    }
    elements.removeAll(menuBarElements);

    final List<Element> panelMailElements = filterElements(elements, PANEL_MAIL);
    for (final Element element : panelMailElements) {
      doInsertPanelMail(element);
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
                      MENU_BAR,
                      baseView.getName(),
                      baseView.getType(),
                      baseDocument,
                      XPathConstants.NODE);

          if (menuBarNode != null) {
            targetNode = menuBarNode;
            position = Position.AFTER;
          } else {
            final Node toolBarNode =
                (Node)
                    evaluateXPath(
                        TOOL_BAR,
                        baseView.getName(),
                        baseView.getType(),
                        baseDocument,
                        XPathConstants.NODE);
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
                      PANEL_MAIL,
                      baseView.getName(),
                      baseView.getType(),
                      baseDocument,
                      XPathConstants.NODE);

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

    doInsert(elements, position, targetNode);
  }

  private Node doInsert(List<Element> elements, Position position, Node targetNode) {
    final Iterator<Element> it = elements.iterator();

    if (!it.hasNext()) {
      return targetNode;
    }

    Node node = doInsert(it.next(), position, targetNode);

    while (it.hasNext()) {
      node = doInsert(it.next(), Position.AFTER, node);
    }

    return node;
  }

  private Node doInsert(Element element, Position position, Node targetNode) {
    final Node newChild = baseDocument.importNode(element, true);
    position.insert(targetNode, newChild);
    return newChild;
  }

  private void doInsertToolBar(Element element) throws XPathExpressionException {
    final List<Element> elements;
    final Node targetNode;
    final Position position;
    final Node toolBarNode =
        (Node)
            evaluateXPath(
                TOOL_BAR,
                baseView.getName(),
                baseView.getType(),
                baseDocument,
                XPathConstants.NODE);

    if (toolBarNode != null) {
      elements = findElements(element.getChildNodes());
      targetNode = toolBarNode;
      position = Position.INSIDE_LAST;
    } else {
      elements = ImmutableList.of(element);
      targetNode =
          (Node)
              evaluateXPath(
                  "/", baseView.getName(), baseView.getType(), baseDocument, XPathConstants.NODE);
      position = Position.INSIDE_FIRST;
    }

    doInsert(elements, position, targetNode);
  }

  private void doInsertMenuBar(Element element) throws XPathExpressionException {
    final List<Element> elements;
    final Node targetNode;
    final Position position;
    final Node menuBarNode =
        (Node)
            evaluateXPath(
                MENU_BAR,
                baseView.getName(),
                baseView.getType(),
                baseDocument,
                XPathConstants.NODE);

    if (menuBarNode != null) {
      elements = findElements(element.getChildNodes());
      targetNode = menuBarNode;
      position = Position.INSIDE_LAST;
    } else {
      elements = ImmutableList.of(element);
      final Node toolBarNode =
          (Node)
              evaluateXPath(
                  TOOL_BAR,
                  baseView.getName(),
                  baseView.getType(),
                  baseDocument,
                  XPathConstants.NODE);

      if (toolBarNode != null) {
        targetNode = toolBarNode;
        position = Position.AFTER;
      } else {
        targetNode =
            (Node)
                evaluateXPath(
                    "/", baseView.getName(), baseView.getType(), baseDocument, XPathConstants.NODE);
        position = Position.INSIDE_FIRST;
      }
    }

    doInsert(elements, position, targetNode);
  }

  private void doInsertPanelMail(Element element) throws XPathExpressionException {
    final List<Element> elements = ImmutableList.of(element);
    final Node targetNode;
    final Position position;
    final Node panelMailNode =
        (Node)
            evaluateXPath(
                PANEL_MAIL,
                baseView.getName(),
                baseView.getType(),
                baseDocument,
                XPathConstants.NODE);

    if (panelMailNode != null) {
      doReplace(elements, panelMailNode);
      return;
    }

    targetNode =
        (Node)
            evaluateXPath(
                "/", baseView.getName(), baseView.getType(), baseDocument, XPathConstants.NODE);
    position = Position.INSIDE_LAST;
    doInsert(elements, position, targetNode);
  }

  private void doReplace(Node extendItemNode, Node targetNode) throws XPathExpressionException {
    final List<Element> elements = findElements(extendItemNode.getChildNodes());
    Node changedTargetNode = null;

    final List<Element> toolBarElements = filterElements(elements, TOOL_BAR);
    for (final Element element : toolBarElements) {
      changedTargetNode = doReplaceToolBar(element);
    }
    elements.removeAll(toolBarElements);

    final List<Element> menuBarElements = filterElements(elements, MENU_BAR);
    for (final Element element : menuBarElements) {
      changedTargetNode = doReplaceMenuBar(element);
    }
    elements.removeAll(menuBarElements);

    final List<Element> panelMailElements = filterElements(elements, PANEL_MAIL);
    for (final Element element : panelMailElements) {
      changedTargetNode = doReplacePanelMail(element);
    }
    elements.removeAll(panelMailElements);

    if (changedTargetNode != null) {
      doInsert(elements, Position.AFTER, changedTargetNode);
    } else {
      doReplace(elements, targetNode);
    }
  }

  @Nullable
  private Node doReplace(List<Element> elements, Node targetNode) {
    if (elements.isEmpty()) {
      targetNode.getParentNode().removeChild(targetNode);
      return null;
    } else {
      final Node node = baseDocument.importNode(elements.get(0), true);
      targetNode.getParentNode().replaceChild(node, targetNode);
      return doInsert(elements.subList(1, elements.size()), Position.AFTER, node);
    }
  }

  private Node doReplaceToolBar(Element element) throws XPathExpressionException {
    final List<Element> elements = ImmutableList.of(element);
    final Node toolBarNode =
        (Node)
            evaluateXPath(
                TOOL_BAR,
                baseView.getName(),
                baseView.getType(),
                baseDocument,
                XPathConstants.NODE);

    if (toolBarNode != null) {
      return doReplace(elements, toolBarNode);
    }

    final Node targetNode =
        (Node)
            evaluateXPath(
                "/", baseView.getName(), baseView.getType(), baseDocument, XPathConstants.NODE);
    final Position position = Position.INSIDE_FIRST;
    return doInsert(elements, position, targetNode);
  }

  private Node doReplaceMenuBar(Element element) throws XPathExpressionException {
    final List<Element> elements = ImmutableList.of(element);
    final Node menuBarNode =
        (Node)
            evaluateXPath(
                MENU_BAR,
                baseView.getName(),
                baseView.getType(),
                baseDocument,
                XPathConstants.NODE);

    if (menuBarNode != null) {
      return doReplace(elements, menuBarNode);
    }

    final Node targetNode;
    final Position position;
    final Node toolBarNode =
        (Node)
            evaluateXPath(
                TOOL_BAR,
                baseView.getName(),
                baseView.getType(),
                baseDocument,
                XPathConstants.NODE);

    if (toolBarNode != null) {
      targetNode = toolBarNode;
      position = Position.AFTER;
    } else {
      targetNode =
          (Node)
              evaluateXPath(
                  "/", baseView.getName(), baseView.getType(), baseDocument, XPathConstants.NODE);
      position = Position.INSIDE_FIRST;
    }

    return doInsert(elements, position, targetNode);
  }

  private Node doReplacePanelMail(Element element) throws XPathExpressionException {
    final List<Element> elements = ImmutableList.of(element);
    final Node panelMailNode =
        (Node)
            evaluateXPath(
                PANEL_MAIL,
                baseView.getName(),
                baseView.getType(),
                baseDocument,
                XPathConstants.NODE);

    if (panelMailNode != null) {
      return doReplace(elements, panelMailNode);
    }

    final Node targetNode =
        (Node)
            evaluateXPath(
                "/", baseView.getName(), baseView.getType(), baseDocument, XPathConstants.NODE);
    final Position position = Position.INSIDE_LAST;
    return doInsert(elements, position, targetNode);
  }

  private void doMove(Node extendItemNode, Node targetNode, MetaView extensionView)
      throws XPathExpressionException {
    final NamedNodeMap attributes = extendItemNode.getAttributes();
    final String source = getNodeAttributeValue(attributes, "source");
    final Node sourceNode =
        (Node)
            evaluateXPath(
                source, baseView.getName(), baseView.getType(), baseDocument, XPathConstants.NODE);

    if (sourceNode == null) {
      LOG.error(
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

  private void doAttribute(Node extendItemNode, Node targetNode) {
    final NamedNodeMap attributes = extendItemNode.getAttributes();
    final String name = getNodeAttributeValue(attributes, "name");
    final String value = getNodeAttributeValue(attributes, "value");

    if (!(targetNode instanceof Element)) {
      LOG.error("Can change attributes only on elements: {}", targetNode);
      return;
    }

    final Element targetElement = ((Element) targetNode);

    if (StringUtils.isEmpty(value)) {
      targetElement.removeAttribute(name);
    } else {
      targetElement.setAttribute(name, value);
    }
  }

  private void addDependentFeature(String featureName) {
    dependentFeatures.add(featureName);
  }

  private void addDependentModule(String moduleName) {
    dependentModules.add(moduleName);
  }

  public Set<String> getDependentFeatures() {
    return dependentFeatures;
  }

  public Set<String> getDependentModules() {
    return dependentModules;
  }
}
