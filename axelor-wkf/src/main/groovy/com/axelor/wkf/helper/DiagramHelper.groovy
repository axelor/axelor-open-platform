/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.wkf.helper

import groovy.util.logging.Slf4j;

import javax.inject.Inject
import javax.xml.parsers.ParserConfigurationException

import org.xml.sax.SAXException

import wslite.json.JSONException
import wslite.json.JSONObject

import com.axelor.wkf.db.Instance

@Slf4j
class DiagramHelper {
	
	@Inject
	protected XmlParser xparse
	
	public String generateDiagram (Instance instance) {
			
		log.debug("Generate diagram for instance ::: {}", instance)
		
		try {
			
			String xmlData = instance.workflow.xmlData
			
			NodeList svgList = (NodeList) xparse.parseText(xmlData)["svg-representation"]
			NodeList jsonList = (NodeList) xparse.parseText(xmlData)["json-representation"]
			
			String svgNode =  ( (NodeList) ( (Node) svgList[0] ).value() )[0]
			String jsonNode = ((NodeList) ( (Node) jsonList[0] ).value() )[0]
			
			return updateDiagram( svgNode, jsonNode, instance.nodes.collect { it.name } )
			
		}
		catch(Exception e){
			
			log.error("${e}")
			return null
			
		}
	}
	
	protected String updateDiagram (String svgNode, String jsonNode, Collection activeNodes) throws Exception {
		
		log.debug("Update diagram for active nodes ::: {}", activeNodes)
		
		JSONObject diagram = new JSONObject(jsonNode)
		def svg = xparse.parseText(svgNode)
		
		for ( String activeNode : activeNodes ) {
			
			for( JSONObject shape : diagram.get("childShapes") ){
				
				if( shape["properties"]["name"] == activeNode ){
					
					def nd = svg.depthFirst().find{ node -> node.attribute('id') == "svg-${shape['resourceId']}" }
					
					def children = nd.depthFirst().findAll{ node -> node.attribute('stroke') != null }
					
					for(child in children) { child.attributes().put("stroke","red") }
					
					break;
					
				}
			}
		}
		
		StringWriter so = new StringWriter()
		PrintWriter po = new PrintWriter(so)	
		XmlNodePrinter pn = new XmlNodePrinter(po)
		pn.print(svg)
		return so.toString()
		
	}
	
}
