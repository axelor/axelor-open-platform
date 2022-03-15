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
package com.axelor.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/** The {@link XMLUtils} provides various methods to deal with XML parsing. */
public class XMLUtils {

    private static final Logger LOG = LoggerFactory.getLogger(XMLUtils.class);

    private static final String PROPERTY_UNSUPPORTED = "Property {} is not supported.";

    // DocumentBuilder block

    /**
     * Returns properly configured {@link DocumentBuilderFactory} with security features and no
     * external DTD and schema access.
     *
     * @param namespaceAware whether the returned factory is to provide support for XML namespaces
     * @return configured {@link DocumentBuilderFactory}
     */
    public static DocumentBuilderFactory createDocumentBuilderFactory(Boolean namespaceAware) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(namespaceAware);

        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException ignore) {
            LOG.trace(PROPERTY_UNSUPPORTED, XMLConstants.FEATURE_SECURE_PROCESSING);
        }

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        } catch (IllegalArgumentException ignore) {
            LOG.trace(PROPERTY_UNSUPPORTED, XMLConstants.ACCESS_EXTERNAL_DTD);
        }

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException ignore) {
            LOG.trace(PROPERTY_UNSUPPORTED, XMLConstants.ACCESS_EXTERNAL_SCHEMA);
        }

        return factory;
    }

    /**
     * Returns properly configured {@link DocumentBuilder} with security features.
     *
     * @return configured {@link DocumentBuilder}
     */
    public static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        return createDocumentBuilderFactory(false).newDocumentBuilder();
    }

    // SAXParser block

    /**
     * Returns properly configured {@link SAXParserFactory} with security features.
     *
     * @param namespaceAware whether the returned factory is to provide support for XML namespaces
     * @return configured {@link SAXParserFactory}
     */
    public static SAXParserFactory createSAXParserFactory(Boolean namespaceAware) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(namespaceAware);

        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (SAXNotRecognizedException
                | ParserConfigurationException
                | SAXNotSupportedException ignore) {
            LOG.trace(PROPERTY_UNSUPPORTED, XMLConstants.FEATURE_SECURE_PROCESSING);
        }

        return factory;
    }

    /**
     * Returns properly configured {@link SAXParser} with security features and no external DTD and
     * schema access.
     *
     * @return configured {@link SAXParser}
     */
    public static SAXParser createSAXParser() throws ParserConfigurationException, SAXException {
        SAXParser parser = createSAXParserFactory(false).newSAXParser();

        try {
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            LOG.trace(PROPERTY_UNSUPPORTED, XMLConstants.ACCESS_EXTERNAL_DTD);
        }

        try {
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            LOG.trace(PROPERTY_UNSUPPORTED, XMLConstants.ACCESS_EXTERNAL_SCHEMA);
        }

        return parser;
    }

    // XPath block

    /**
     * Returns properly configured {@link XPathFactory} with security features.
     *
     * @return configured {@link XPathFactory}
     */
    public static XPathFactory createXPathFactory() {
        XPathFactory factory = XPathFactory.newInstance();

        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (XPathFactoryConfigurationException ignore) {
            LOG.trace(PROPERTY_UNSUPPORTED, XMLConstants.FEATURE_SECURE_PROCESSING);
        }

        return factory;
    }

    /**
     * Returns properly configured {@link XPath} with security features
     *
     * @return configured {@link XPath}
     */
    public static XPath createXPath() {
        return createXPathFactory().newXPath();
    }

    // Transformer block

    /**
     * Returns properly configured {@link TransformerFactory} with security features.
     *
     * @return configured {@link TransformerFactory}
     */
    public static TransformerFactory createTransformerFactory() {
        TransformerFactory factory = TransformerFactory.newInstance();

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        } catch (Exception e) {
            LOG.trace(PROPERTY_UNSUPPORTED, XMLConstants.ACCESS_EXTERNAL_DTD);
        }

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (Exception e) {
            LOG.trace(PROPERTY_UNSUPPORTED, XMLConstants.ACCESS_EXTERNAL_STYLESHEET);
        }

        return factory;
    }

    // DOM helpers

    private static Document parse(ParserFunction parser)
            throws ParserConfigurationException, SAXException, IOException {
        return parser.apply(createDocumentBuilderFactory(true).newDocumentBuilder());
    }

    /**
     * Parse the content of the given file as an XML document.
     *
     * @param file the file to parse
     * @return A new DOM Document object.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @see DocumentBuilder#parse(File)
     */
    public static Document parse(File file)
            throws ParserConfigurationException, SAXException, IOException {
        return parse(builder -> builder.parse(file));
    }

    /**
     * Parse the content of the given {@link InputStream} as an XML document.
     *
     * @param is the InputStream to parse
     * @return A new DOM Document object.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @see DocumentBuilder#parse(InputStream)
     */
    public static Document parse(InputStream is)
            throws ParserConfigurationException, SAXException, IOException {
        return parse(builder -> builder.parse(is));
    }

    /**
     * Parse the content of the given {@link InputSource} as an XML document.
     *
     * @param is the InputSource to parse
     * @return A new DOM Document object.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @see DocumentBuilder#parse(InputSource)
     */
    public static Document parse(InputSource is)
            throws ParserConfigurationException, SAXException, IOException {
        return parse(builder -> builder.parse(is));
    }

    /**
     * Parse the content of the given {@link Reader} as an XML document.
     *
     * @param reader the Reader to parse
     * @return A new DOM Document object.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static Document parse(Reader reader)
            throws ParserConfigurationException, SAXException, IOException {
        return parse(new InputSource(reader));
    }

    /**
     * Parse the content of the given URI as an XML document.
     *
     * @param uri the location of the content to be parsed.
     * @return A new DOM Document object.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @see DocumentBuilder#parse(String)
     */
    public static Document parse(URI uri)
            throws ParserConfigurationException, SAXException, IOException {
        return parse(builder -> builder.parse(uri.toString()));
    }

    /**
     * Transform the given node to xml content.
     *
     * @param node the node to transform
     * @param writer the writer where to write the content
     * @param encoding encoding to user for xml content
     * @throws TransformerException
     */
    public static void transform(Node node, Writer writer, String encoding)
            throws TransformerException {
        final Transformer transformer;
        try {
            transformer = createTransformerFactory().newTransformer();
        } catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
            throw new RuntimeException(e);
        }
        transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
        transformer.transform(new DOMSource(node), new StreamResult(writer));
    }

    /**
     * Create an {@link Iterator} for the given nodes.
     *
     * @param nodes the nodes
     * @return an iterator
     */
    public static Iterator<Node> iterator(NodeList nodes) {
        return new NodeIterator(nodes);
    }

    /**
     * Create a {@link Spliterator} for the given nodes.
     *
     * @param nodes the nodes
     * @return a spliterator
     */
    public static Spliterator<Node> spliterator(NodeList nodes) {
        return Spliterators.spliterator(iterator(nodes), nodes.getLength(), Spliterator.ORDERED);
    }

    /**
     * Create a {@link Stream} of the nodes.
     *
     * @param nodes the nodes
     * @return stream of the nodes
     */
    public static Stream<Node> stream(NodeList nodes) {
        return StreamSupport.stream(spliterator(nodes), false);
    }

    /**
     * Create a {@link Stream} of the child elements with the given tag name.
     *
     * @param node the parent node
     * @param tagName the tag name of the child nodes, use <code>"*"</code> for any elements
     * @return stream of the matching child elements
     */
    public static Stream<Element> stream(Node node, String tagName) {
        return stream(node.getChildNodes())
                .filter(Element.class::isInstance)
                .map(Element.class::cast)
                .filter(e -> matchTagName(e, tagName));
    }

    private static boolean matchTagName(Element element, String tagName) {
        if (StringUtils.isBlank(tagName)) return false;
        if ("*".equals(tagName)) return true;
        if (tagName.contains(":")) return element.getTagName().equals(tagName);
        return element.getLocalName().equals(tagName);
    }

    @FunctionalInterface
    private static interface ParserFunction {
        Document apply(DocumentBuilder builder) throws SAXException, IOException;
    }

    private static final class NodeIterator implements Iterator<Node> {

        private final NodeList nodes;
        private final int length;
        private int index = 0;

        private NodeIterator(NodeList nodes) {
            this.nodes = nodes;
            this.length = nodes.getLength();
        }

        @Override
        public boolean hasNext() {
            return index < length;
        }

        @Override
        public Node next() {
            return nodes.item(index++);
        }
    }
}