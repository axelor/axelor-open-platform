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
package com.axelor.meta.loader;

import com.axelor.app.AppConfig;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.StringUtils;
import com.axelor.common.XMLUtils;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.views.Position;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
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

public abstract class AbstractXmlProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractXmlProcessor.class);

  static final String TOOL_BAR = "toolbar";
  static final String MENU_BAR = "menubar";
  static final String PANEL_MAIL = "panel-mail";
  static final Map<Position, Position> ROOT_NODE_POSITION_MAP =
      ImmutableMap.of(Position.AFTER, Position.INSIDE_LAST, Position.BEFORE, Position.INSIDE_FIRST);

  static AppConfig appConfigProvider;

  static {
    final String appConfigProdiverName =
        AppSettings.get().get(AvailableAppSettings.APPLICATION_CONFIG_PROVIDER);

    if (StringUtils.notBlank(appConfigProdiverName)) {
      try {
        @SuppressWarnings("unchecked")
        final Class<AppConfig> cls = (Class<AppConfig>) Class.forName(appConfigProdiverName);
        appConfigProvider = Beans.get(cls);
      } catch (ClassNotFoundException e) {
        LOG.error(
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

  public static final LoadingCache<String, XPathExpression> XPATH_EXPRESSION_CACHE =
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

  public Object evaluateXPath(
      String subExpression, String name, String type, Object item, QName returnType)
      throws XPathExpressionException {
    return evaluateXPath(prepareXPathExpression(subExpression, name, type), item, returnType);
  }

  private String prepareXPathExpression(String subExpression, String name, String type) {
    final String rootExpr = "/:object-views/:%s[@name='%s']";
    final String expr = subExpression.startsWith("/") ? subExpression.substring(1) : subExpression;
    return String.format(expr.isEmpty() ? rootExpr : rootExpr + "/" + expr, type, name, expr);
  }

  private Object evaluateXPath(String expression, Object item, QName returnType)
      throws XPathExpressionException {
    XPathExpression xPathExpression = XPATH_EXPRESSION_CACHE.getUnchecked(expression);

    synchronized (xPathExpression) {
      return xPathExpression.evaluate(item, returnType);
    }
  }

  public List<Element> findElements(NodeList nodeList) {
    return nodeListToStream(nodeList)
        .filter(node -> node instanceof Element)
        .map(node -> (Element) node)
        .collect(Collectors.toList());
  }

  public List<Element> filterElements(List<Element> elements, String nodeName) {
    return elements.stream()
        .filter(element -> nodeName.equals(element.getNodeName()))
        .collect(Collectors.toList());
  }

  public Node findViewNode(Document document) {
    return nodeListToStream(document.getFirstChild().getChildNodes())
        .filter(node -> node instanceof Element)
        .findFirst()
        .orElseThrow(NoSuchElementException::new);
  }

  public Stream<Node> nodeListToStream(NodeList nodeList) {
    return nodeListToList(nodeList).stream();
  }

  public List<Node> nodeListToList(NodeList nodeList) {
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

  public String getNodeAttributeValue(NamedNodeMap attributes, String name) {
    final Node item = attributes.getNamedItem(name);
    return item != null ? item.getNodeValue() : "";
  }
}
