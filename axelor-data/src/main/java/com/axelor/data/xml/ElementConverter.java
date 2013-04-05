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
