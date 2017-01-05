/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.wkf.helper;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.InputSource;

import wslite.json.JSONObject;

import com.axelor.wkf.db.Instance;

public class DiagramHelper {

	protected Logger log = LoggerFactory.getLogger( getClass() );
	
	public String generateDiagram (Instance instance) {
			
		log.debug("Generate diagram for instance ::: {}", instance);
		
		try {
			
			Document document = createDocument( instance.getWorkflow().getXmlData() );
			
			NodeList svgNodeList = document.getElementsByTagName("svg-representation");
			NodeList jsonNodeList = document.getElementsByTagName("json-representation");
			
			String svg = svgNodeList.item(0).getChildNodes().item(0).getNodeValue();
			String json = jsonNodeList.item(0).getChildNodes().item(0).getNodeValue();

			if ( instance.getNodes() == null || instance.getNodes().isEmpty() ) { return ""; }
			
			return updateDiagram( svg, json, instance.getNodes() );
			
			
		} catch(Exception e){
			
			log.error( "{}", e );
			return null;
			
		}
	}
	
	@SuppressWarnings("unchecked")
	protected String updateDiagram ( String svg, String json, Collection<com.axelor.wkf.db.Node> activeNodes) throws Exception {
		
		log.debug("Update diagram for active nodes ::: {}", activeNodes);
		
		JSONObject diagram = new JSONObject( json );
		Document document = createDocument( svg );
		
		for ( com.axelor.wkf.db.Node activeNode : activeNodes ) {
			for( JSONObject shape : ( Collection<JSONObject> ) diagram.get("childShapes") ){
				if( ! ( ( Map<?,?> ) shape.get( "properties" ) ).get("name").equals( activeNode.getName() ) ){ continue; }
				setStrokes( document, getResource( document, shape.get("resourceId") ) );
				break;
			}
		}
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();

		StringWriter stringWriter = new StringWriter();
		StreamResult streamResult = new StreamResult( stringWriter );
		
		DOMSource source = new DOMSource( document );
		transformer.transform( source, streamResult);
		
		return stringWriter.toString();
		
	}
	
	protected Node getResource( Document document, Object resource ) throws Exception {
		
		if ( document == null || resource == null ) { return null; }
		
		String attributeId = "id", attribute = String.format( "svg-%s", resource );
		
		NodeIterator nodeIterator = createNodeIterator(document);
		
		Node node;
		while ( ( node = nodeIterator.nextNode() ) != null ){
			if ( !node.hasAttributes() ) { continue; } 
			Node attributeNode = node.getAttributes().getNamedItem( attributeId );
			if ( attributeNode == null || !attributeNode.getTextContent().equals(attribute) ) { continue; };
			return node;
		}
		
		return null;
		
	}
	
	protected void setStrokes( Document document, Node root ) throws Exception {
		
		if ( document == null || root == null ) { return; }
		
		String attributeId = "stroke";
		NodeIterator nodeIterator = createNodeIterator( document, root );

		Node node;
		while ( ( node = nodeIterator.nextNode() ) != null ){
			if ( !node.hasAttributes() ) { continue; }
			Node attribute = node.getAttributes().getNamedItem( attributeId );
			if ( attribute == null ) { continue; }
			attribute.setTextContent("red"); 
			
		}
		
	}
	
	protected NodeIterator createNodeIterator( Document document ) throws Exception {
		
		return createNodeIterator(document, document.getDocumentElement());
	}
	
	protected NodeIterator createNodeIterator( Document document, Node root ) throws Exception {
		
		return ((DocumentTraversal)document).createNodeIterator( root, NodeFilter.SHOW_ALL, null, true );
	}
	
	protected Document createDocument( String xml ) throws Exception {
		
		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

		return documentBuilder.parse( new InputSource( new StringReader( xml ) ) );
		
	}
	
	protected void logAttributes ( Node node ){

		log.debug("Show attributes for node {}", node.getNodeName() );
		NamedNodeMap attributeNode = node.getAttributes();
		for (int i = 0; i < attributeNode.getLength(); i++) {
			
			Node attribute = attributeNode.item(i);
			log.debug("Attribute {} = {} ", attribute.getNodeName(), attribute.getNodeValue());
			
		}
		
	}
	
}
