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
			if (MetaSequence.findByName(sequence.getName()) != null) {
				continue;
			}
			
			MetaSequence entity = new MetaSequence(sequence.getName());
			
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
