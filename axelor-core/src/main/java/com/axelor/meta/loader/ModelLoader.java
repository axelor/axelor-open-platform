/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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
package com.axelor.meta.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.db.JPA;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.db.MetaSequence;
import com.axelor.meta.domains.DomainModels;
import com.axelor.meta.domains.Sequence;
import com.axelor.meta.service.MetaModelService;
import com.google.common.base.Throwables;
import com.google.common.io.Resources;

@Singleton
public class ModelLoader extends AbstractLoader {

	private static Logger log = LoggerFactory.getLogger(ModelLoader.class);

	private static final String LOCAL_SCHEMA_DOMAIN = "domain-models.xsd";
	private static Unmarshaller unmarshaller;
	
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
				DomainModels models = unmarshal(file.openStream());
				if (models.getSequences() != null) {
					log.info("importing sequence data: {}", file.getFile());
					importSequences(models.getSequences(), update);
				}
			} catch (IOException | JAXBException e) {
				throw Throwables.propagate(e);
			}
		}
	}
	
	private void importSequences(List<Sequence> sequences, boolean update) {
		
		for (Sequence sequence : sequences) {
			if (isVisited(Sequence.class, sequence.getName())) {
				continue;
			}
			log.info("importing sequence: {}", sequence.getName());
			MetaSequence entity = MetaSequence.findByName(sequence.getName());
			if (entity == null) {
				entity = new MetaSequence(sequence.getName());
			}
			
			if (isUpdated(entity)) {
				continue;
			}
			
			entity.setPrefix(sequence.getPrefix());
			entity.setSuffix(entity.getSuffix());
			if (sequence.getPadding() != null) {
				entity.setPadding(sequence.getPadding());
			}
			if (sequence.getInitial() != null) {
				entity.setInitial(sequence.getInitial());
			}
			if (sequence.getIncrement() != null) {
				entity.setIncrement(sequence.getIncrement());
			}
			entity.save();
		}
	}
	
	private DomainModels unmarshal(InputStream stream) throws JAXBException {
		if (unmarshaller == null) {
			init();
		}
		synchronized (unmarshaller) {
			return (DomainModels) unmarshaller.unmarshal(stream);
		}
	}

	private static void init() {
		try {
			JAXBContext context = JAXBContext.newInstance(DomainModels.class);
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemaFactory.newSchema(Resources.getResource(LOCAL_SCHEMA_DOMAIN));
			
			unmarshaller = context.createUnmarshaller();
			unmarshaller.setSchema(schema);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}
}
