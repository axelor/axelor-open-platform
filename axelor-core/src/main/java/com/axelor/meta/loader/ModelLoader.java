package com.axelor.meta.loader;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.axelor.db.JPA;
import com.axelor.meta.service.MetaModelService;

@Singleton
public class ModelLoader implements Loader {

	@Inject
	private MetaModelService service;

	@Override
	public void load(Module module) {
		
		for (Class<?> klass : JPA.models()) {
			if (module.hasEntity(klass)) {
				service.process(klass);
			}
		}
	}
}
