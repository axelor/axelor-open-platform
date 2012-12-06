package com.axelor.web.service;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.axelor.web.AppSettings;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/app")
public class AppService extends AbstractService {

	@Inject
	private AppSettings settings;
	
	@GET
	@Path("info")
	public String info() {
		return settings.toJSON();
	}
}
