/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.data.xml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.DomWriter;

public class ElementConverter implements Converter {
	
	private XMLBinder binder;

	public ElementConverter(XMLBinder binder) {
		this.binder = binder;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean canConvert(Class type) {
		return Document.class.isAssignableFrom(type) || Element.class.isAssignableFrom(type);
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
	}
	
	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		
		if (Document.class.isAssignableFrom(context.getRequiredType())) {
			String last = null;
			while(reader.hasMoreChildren()) {
				reader.moveDown();
				Document node = (Document) context.convertAnother(reader, Element.class);
				if (last != null && !last.equals(node.getFirstChild().getNodeName())) {
					binder.finish();
					last = node.getFirstChild().getNodeName();
				}
				if (last == null) {
					last = node.getFirstChild().getNodeName();
				}
				reader.moveUp();
				binder.bind(node);
			}
			return null;
		}

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ConversionException("Cannot instantiate " + Document.class.getName(), e);
        }
        Document document = builder.newDocument();
        DomWriter writer = new DomWriter(document, new NoNameCoder());
        
        copy(reader, writer);
        
        return document;
	}
	
	private static void copy(HierarchicalStreamReader reader, HierarchicalStreamWriter writer) {
        writer.startNode(reader.getNodeName());

        // write the attributes
        int attributeCount = reader.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            String attributeName = reader.getAttributeName(i);
            String attributeValue = reader.getAttribute(i);
            writer.addAttribute(attributeName, attributeValue);
        }

        // write the child nodes recursively
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            copy(reader, writer);
            reader.moveUp();
        }

        // write the context if any
        String value = reader.getValue();
        if (value != null && value.trim().length() > 0) {
            writer.setValue(value);
        }

        writer.endNode();
    }
}
