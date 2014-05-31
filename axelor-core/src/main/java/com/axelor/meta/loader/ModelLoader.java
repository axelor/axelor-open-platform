/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.axelor.db.JPA;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.db.MetaSequence;
import com.axelor.meta.service.MetaModelService;
import com.google.common.base.Throwables;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

public class ModelLoader extends AbstractLoader {

	private static Logger log = LoggerFactory.getLogger(ModelLoader.class);
	
	@Inject
	private MetaModelService service;

	@Override
	protected void doLoad(Module module, boolean update) {

		for (Class<?> klass : JPA.models()) {
			if (module.hasEntity(klass)) {
				service.process(klass);
			}
		}
		
		loadSequences(module, update);
	}

	private void loadSequences(Module module, boolean update) {
		for (URL file : MetaScanner.findAll(module.getName(), "domains", "(.*?)\\.xml")) {
			try {
				importSequences(file, update);
			} catch (Exception e) {
				throw Throwables.propagate(e);
			}
		}
	}
	
	private void importSequences(URL file, boolean update)
			throws IOException, ParserConfigurationException {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = null;

		try (InputStream is = file.openStream()){
			doc = db.parse(is);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}

		NodeList elements = doc.getElementsByTagName("sequence");
		
		if (elements.getLength() == 0) {
			return;
		}
		
		log.info("importing sequence data: {}", file);
		
		for (int i = 0; i < elements.getLength(); i++) {
			
			Element element = (Element) elements.item(i);
			String name = element.getAttribute("name");
			
			if (isVisited(MetaSequence.class, name)) {
				continue;
			}
			if (MetaSequence.findByName(name) != null) {
				continue;
			}
			
			log.info("importing sequence: {}", name);
			
			MetaSequence entity = new MetaSequence(name);
			
			entity.setPrefix(element.getAttribute("prefix"));
			entity.setSuffix(element.getAttribute("suffix"));
			
			Integer padding = Ints.tryParse(element.getAttribute("padding"));
			Integer increment = Ints.tryParse(element.getAttribute("padding"));
			Long initial = Longs.tryParse(element.getAttribute("initial"));

			if (padding != null) entity.setPadding(padding);
			if (increment != null) entity.setIncrement(increment);
			if (initial != null) entity.setInitial(initial);
			
			entity.save();
		}
	}
}
