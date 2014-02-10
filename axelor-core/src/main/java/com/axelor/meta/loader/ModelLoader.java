package com.axelor.meta.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.reflections.vfs.Vfs;
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
public class ModelLoader implements Loader {

	private static Logger log = LoggerFactory.getLogger(ModelLoader.class);

	private static final String LOCAL_SCHEMA_DOMAIN = "domain-models_1.0.xsd";
	private static Unmarshaller unmarshaller;
	
	@Inject
	private MetaModelService service;

	@Override
	public void load(Module module) {
		
		for (Class<?> klass : JPA.models()) {
			if (module.hasEntity(klass)) {
				service.process(klass);
			}
		}
		
		loadSequences(module);
	}

	private void loadSequences(Module module) {

		for (Vfs.File file : MetaScanner.findAll(module.getName(), "domains", "(.*?)\\.xml")) {
			try {
				DomainModels models = unmarshal(file.openInputStream());
				if (models.getSequences() != null) {
					log.info("importing sequence data: {}", file.getName());
					importSequences(models.getSequences());
				}
			} catch (IOException | JAXBException e) {
				throw Throwables.propagate(e);
			}
		}
	}
	
	private void importSequences(List<Sequence>  sequences) {
		for (Sequence sequence : sequences) {
			log.info("importing sequence: {}", sequence.getName());
			MetaSequence entity = MetaSequence.findByName(sequence.getName());
			if (entity == null) {
				entity = new MetaSequence(sequence.getName());
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
