package com.axelor.web.service;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

public abstract class AbstractService {

	protected Logger LOG = LoggerFactory.getLogger(getClass());

	@Context
	private UriInfo uriInfo;

	@Inject
	private Injector injector;

	protected final UriInfo getUriInfo() {
		return uriInfo;
	}
	
	protected final Injector getInjector() {
		return injector;
	}
}
