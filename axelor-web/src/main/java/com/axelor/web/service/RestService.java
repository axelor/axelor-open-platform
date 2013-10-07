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
package com.axelor.web.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.service.MetaService;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.axelor.web.AppSettings;
import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.servlet.RequestScoped;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataParam;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/rest/{model}")
public class RestService extends ResourceService {

	@Inject
	private MetaService service;

	@GET
	public Response find(
			@QueryParam("limit")
			@DefaultValue("40") int limit,
			@QueryParam("offset")
			@DefaultValue("0") int offset,
			@QueryParam("q") String query) {

		Request request = new Request();
		request.setOffset(offset);
		request.setLimit(limit);
		return getResource().search(request);
	}

	@POST
	@Path("search")
	public Response find(Request request) {
		request.setModel(getModel());
		return getResource().search(request);
	}

	@POST
	public Response save(Request request) {
		request.setModel(getModel());
		return getResource().save(request);
	}

	@PUT
	public Response create(Request request) {
		request.setModel(getModel());
		return getResource().save(request);
	}

	@GET
	@Path("{id}")
	public Response read(
			@PathParam("id") long id) {
		return getResource().read(id);
	}

	@POST
	@Path("{id}/fetch")
	public Response fetch(
			@PathParam("id") long id, Request request) {
		return getResource().fetch(id, request);
	}

	@POST
	@Path("{id}")
	public Response update(@PathParam("id") long id, Request request) {
		request.setModel(getModel());
		return getResource().save(request);
	}

	@DELETE
	@Path("{id}")
	public Response delete(@PathParam("id") long id, @QueryParam("version") int version) {
		Request request = new Request();
		request.setModel(getModel());
		request.setData(ImmutableMap.of("id", (Object) id, "version", version));
		return getResource().remove(id, request);
	}

	@POST
	@Path("{id}/remove")
	public Response remove(@PathParam("id") long id, Request request) {
		request.setModel(getModel());
		return getResource().remove(id, request);
	}

	@POST
	@Path("removeAll")
	public Response remove(Request request) {
		request.setModel(getModel());
		return getResource().remove(request);
	}

	@GET
	@Path("{id}/copy")
	public Response copy(@PathParam("id") long id) {
		return getResource().copy(id);
	}

	@GET
	@Path("{id}/details")
	public Response details(@PathParam("id") long id) {
		Request request = new Request();
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("id", id);
		request.setModel(getModel());
		request.setData(data);

		return getResource().getRecordName(request);
	}

	@POST
	@Path("upload")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response upload(
			@FormDataParam("request") FormDataBodyPart requestText,
			@FormDataParam("field") String field,
			@FormDataParam("file") InputStream fileStream,
			@FormDataParam("file") FormDataContentDisposition fileDetails) throws IOException {

		requestText.setMediaType(MediaType.APPLICATION_JSON_TYPE);

		Request request = requestText.getEntityAs(Request.class);
		request.setModel(getModel());

		Map<String, Object> values = request.getData();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int read = 0;
		byte[] bytes = new byte[1024];

		while ((read = fileStream.read(bytes)) != -1) {
			out.write(bytes, 0, read);
		}

		values.put(field, out.toByteArray());

		return getResource().save(request);
	}

	@GET
	@Path("{id}/{field}/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@SuppressWarnings("all")
	public javax.ws.rs.core.Response download(
			@PathParam("id") Long id,
			@PathParam("field") String field) {

		Class klass = getResource().getModel();
		Mapper mapper = Mapper.of(klass);
		Model bean = JPA.find(klass, id);
		Property prop = mapper.getNameField();
		Object data = mapper.get(bean, field);
		String name = getModel() + "_" + field;
		if(prop != null){
			name = prop.get(bean) != null ? prop.get(bean).toString() : name;
			if(!prop.getName().equals("fileName")){
				name = name.replaceAll("\\s", "") + "_" + id;
				name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
			}
		}

		if (data == null) {
			return javax.ws.rs.core.Response.noContent().build();
		}
		return javax.ws.rs.core.Response.ok(data).header("Content-Disposition", "attachment; filename=" + name).build();
	}

	@POST
	@Path("{id}/attachment")
	public Response attachment(@PathParam("id") long id, Request request){
		return service.getAttachment(id, getModel(), request);
	}

	@POST
	@Path("removeAttachment")
	public Response removeAttachment(Request request) {
		request.setModel(getModel());
		return service.removeAttachment(request);
	}

	@POST
	@Path("{id}/addAttachment")
	public Response addAttachment(@PathParam("id") long id, Request request) {
		request.setModel(getModel());
		return service.addAttachment(id, request);
	}

	@POST
	@Path("verify")
	public Response verify(Request request) {
		request.setModel(getModel());
		return getResource().verify(request);
	}

	@GET
	@Path("perms")
	public Response perms(@QueryParam("action") String action, @QueryParam("id") Long id) {
		if (action != null && id != null) {
			return getResource().perms(id, action);
		}
		if (id != null) {
			return getResource().perms(id);
		}
		return getResource().perms();
	}

	private static final Cache<String, Request> EXPORT_REQUESTS = CacheBuilder
			.newBuilder()
			.expireAfterAccess(1, TimeUnit.MINUTES)
			.build();

	private static Charset csvCharset = Charsets.UTF_8;
	static {
		try {
			csvCharset = Charset.forName(
					AppSettings.get().get("data.export.encoding"));
		} catch (Exception e) {
		}
	}

	@GET
	@Path("export/{name}")
	@Produces("text/csv")
	public StreamingOutput export(@PathParam("name") final String name) {

		final Request request = EXPORT_REQUESTS.getIfPresent(name);
		if (request == null) {
			throw new IllegalArgumentException(name);
		}

		return new StreamingOutput() {

			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				Writer writer = new OutputStreamWriter(output, csvCharset);
				try {
					getResource().export(request, writer);
				} finally {
					writer.close();
					EXPORT_REQUESTS.invalidate(name);
				}
			}
		};
	}

	@POST
	@Path("export")
	public Response export(Request request) {

		Response response = new Response();
		Map<String, Object> data = Maps.newHashMap();
		String fileName = Math.abs(new SecureRandom().nextLong()) + ".csv";

		EXPORT_REQUESTS.put(fileName, request);

		request.setModel(getModel());
		response.setData(data);

		data.put("fileName", fileName);

		return response;
	}

}