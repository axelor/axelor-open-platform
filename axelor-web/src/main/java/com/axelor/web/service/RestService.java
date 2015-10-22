/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
package com.axelor.web.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.DatatypeConverter;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.common.ClassUtils;
import com.axelor.common.FileUtils;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.Repository;
import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailAddress;
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailFollowerRepository;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.axelor.mail.web.MailController;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaFileRepository;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.service.MetaService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/rest/{model}")
public class RestService extends ResourceService {

	@Inject
	private MetaService service;
	
	@Inject
	private MailMessageRepository messages;

	@Inject
	private MailFollowerRepository followers;

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
	@SuppressWarnings("all")
	public Response find(Request request) {
		
		request.setModel(getModel());

		final Map<String, Object> data = request.getData();
		if (data == null || !data.containsKey("_domainAction")) {
			return getResource().search(request);
		}

		// domain actions are used by portlets to evaluate view context
		// in the context of the current form

		final String action = (String) data.get("_domainAction");
		final Action act = MetaStore.getAction(action);
		final ActionRequest actRequest = new ActionRequest();
		
		actRequest.setModel(getModel());
		actRequest.setAction(action);
		actRequest.setData(data);

		final ActionHandler actHandler = new ActionHandler(actRequest);
		final Object res = act.evaluate(actHandler);

		if (res instanceof Map) {
			Map<String, Object> old = (Map) data.get("_domainContext");
			Map<String, Object> ctx = (Map) ((Map) res).get("context");
			if (old == null) {
				old = ctx;
			} else {
				old.putAll(ctx);
			}
		}
		
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
	public Response fetch(@PathParam("id") long id, Request request) {
		request.setModel(getModel());
		Response response = getResource().fetch(id, request);
		long attachments = Query.of(MetaAttachment.class)
				.filter("self.objectId = ?1 AND self.objectName = ?2", id, getModel())
				.cacheable().count();

		if(response.getItem(0) != null) {
			@SuppressWarnings("all")
			Map<String, Object> item = (Map) response.getItem(0);
			item.put("$attachments", attachments);
		}

		return response;
	}

	@POST
	@Path("{id}")
	public Response update(@PathParam("id") long id, Request request) {
		final List<Object> records = new ArrayList<>();
		final Map<String, Object> data = request.getData();
		data.put("id", id);
		records.add(data);

		request.setRecords(records);
		request.setModel(getModel());

		return getResource().save(request);
	}

	@POST
	@Path("updateMass")
	public Response updateMass(Request request) {
		request.setModel(getModel());
		return getResource().updateMass(request);
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
	public Response details(@PathParam("id") long id, @QueryParam("name") String name) {
		Request request = new Request();
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("id", id);
		request.setModel(getModel());
		request.setData(data);
		request.setFields(Lists.newArrayList(name));

		return getResource().getRecordName(request);
	}
	
	private static final String DEFAULT_UPLOAD_PATH = "{java.io.tmpdir}/axelor/attachments";
	private static String uploadPath = AppSettings.get().getPath("file.upload.dir", DEFAULT_UPLOAD_PATH);

	private void uploadSave(InputStream in, OutputStream out) throws IOException {
		int read = 0;
		byte[] bytes = new byte[1024];
		while ((read = in.read(bytes)) != -1) {
			out.write(bytes, 0, read);
		}
		out.close();
	}

	private String getFileName(MultivaluedMap<String, String> headers) {
        final String[] parts = headers.getFirst("Content-Disposition").split(";");
        for (String filename : parts) {
            if ((filename.trim().startsWith("filename"))) {
                String name = filename.split("=")[1].trim();
                // remove quotes
                return name.replaceAll("^\"|\"$", "");
            }
        }
        return null;
    }

	@POST
	@Path("upload")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response upload(final MultipartFormDataInput input) throws IOException {

		final Map<String, List<InputPart>> formData = input.getFormDataMap();
		final InputPart filePart = formData.get("file").get(0);
		final InputPart requestPart = formData.get("request").get(0);
		final InputPart fieldPart = formData.get("field").get(0);

		final boolean isAttachment = MetaFile.class.getName().equals(getModel());
		final String field = fieldPart.getBodyAsString();
		final String fileName = getFileName(filePart.getHeaders());
		final InputStream fileStream = filePart.getBody(InputStream.class, null);
		final Request request = Beans.get(ObjectMapper.class).readValue(requestPart.getBodyAsString(), Request.class);

		request.setModel(getModel());

		final Map<String, Object> data = request.getData();

		if (!isAttachment) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			uploadSave(fileStream, out);
			data.put(field, out.toByteArray());
			return getResource().save(request);
		}

		final MetaFiles files = Beans.get(MetaFiles.class);
		final MetaFileRepository repo = Beans.get(MetaFileRepository.class);
		final MetaFile metaFile = Mapper.toBean(MetaFile.class, data);

		MetaFile entity = metaFile;
		if (metaFile.getId() != null) {
			entity = repo.find(metaFile.getId());
		}

		entity.setFileName(metaFile.getFileName());
		entity.setFileType(metaFile.getFileType());

		File tmp = files.upload(fileStream, 0, -1, fileName);
		entity = files.upload(tmp, entity);

		final Response response = new Response();
		response.setData(Arrays.asList(entity));
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}

	private static final String BLANK_IMAGE = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

	@GET
	@Path("{id}/{field}/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@SuppressWarnings("all")
	public javax.ws.rs.core.Response download(
			@PathParam("id") Long id,
			@PathParam("field") String field,
			@QueryParam("image") boolean isImage) throws IOException {

		boolean isAttachment = MetaFile.class.getName().equals(getModel());

		Class klass = getResource().getModel();
		Mapper mapper = Mapper.of(klass);
		Model bean = JPA.find(klass, id);

		if (isAttachment) {
			final String fileName = (String) mapper.get(bean, "fileName");
			final String filePath = (String) mapper.get(bean, "filePath");
			final File inputFile = FileUtils.getFile(uploadPath, filePath);
			if (!inputFile.exists()) {
				return javax.ws.rs.core.Response.status(Status.NOT_FOUND).build();
			}
			return javax.ws.rs.core.Response.ok(new StreamingOutput() {

				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					uploadSave(new FileInputStream(inputFile), output);
				}
			})
			.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
			.build();
		}

		String fileName = getModel() + "_" + field;
		Object data = mapper.get(bean, field);

		if (isImage) {
			String base64 = BLANK_IMAGE;
			if (data instanceof byte[]) {
				base64 = new String((byte[]) data);
			}
			try {
				base64 = base64.substring(base64.indexOf(";base64,") + 8);
				data = DatatypeConverter.parseBase64Binary(base64);
			} catch (Exception e) {
			}
			return javax.ws.rs.core.Response.ok(data).build();
		}

		fileName = fileName.replaceAll("\\s", "") + "_" + id;
		fileName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fileName);

		if (data == null) {
			return javax.ws.rs.core.Response.noContent().build();
		}

		return javax.ws.rs.core.Response
				.ok(data)
				.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
				.build();
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
		return service.removeAttachment(request, uploadPath);
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

	private static Charset csvCharset = Charsets.ISO_8859_1;
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

		final java.nio.file.Path temp = MetaFiles.findTempFile(name);
		if (Files.notExists(temp)) {
			throw new IllegalArgumentException(name);
		}

		return new StreamingOutput() {

			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				try (final InputStream is = new FileInputStream(temp.toFile())) {
					uploadSave(is, output);
				} finally {
					Files.deleteIfExists(temp);
				}
			}
		};
	}

	@POST
	@Path("export")
	public Response export(Request request) {
		final Response response = new Response();
		final Map<String, Object> data = new HashMap<>();
		try {
			final java.nio.file.Path tempFile = MetaFiles.createTempFile(null, ".csv");
			try(final OutputStream os = new FileOutputStream(tempFile.toFile())) {
				try (final Writer writer = new OutputStreamWriter(os, csvCharset)) {
					getResource().export(request, writer);
				}
			};
			data.put("fileName", tempFile.toFile().getName());
			response.setData(data);
		} catch (IOException e) {
			response.setException(e);
		}

		return response;
	}

	@GET
	@Path("messages")
	public Response messages(
			@QueryParam("folder") String folder,
			@QueryParam("parent") Long parentId,
			@QueryParam("count") boolean count,
			@QueryParam("limit") @DefaultValue("10") Integer limit,
			@QueryParam("offset") @DefaultValue("0") Integer offset,
			@QueryParam("relatedId") Long relatedId,
			@QueryParam("relatedModel") String relatedModel) {

		final MailController ctrl = Beans.get(MailController.class);

		final ActionRequest req = new ActionRequest();
		final ActionResponse res = new ActionResponse();

		req.setModel(relatedModel);
		req.setOffset(offset);
		req.setLimit(limit);

		Class<?> relatedClass = null;
		Model related = null;

		try {
			relatedClass = ClassUtils.findClass(relatedModel);
		} catch (Exception e) {}

		if (relatedClass != null && relatedId != null) {
			@SuppressWarnings("all")
			JpaRepository<?> repo = JpaRepository.of((Class) relatedClass);
			if (repo != null) {
				related = repo.find(relatedId);
			}
		}

		if (related != null) {
			final List<Object> records = new ArrayList<>();
			records.add(related);
			req.setRecords(records);
			ctrl.related(req, res);
			return res;
		}

		if (count) {
			ctrl.unread(req, res);
			return res;
		}
		
		if (parentId != null) {
			List<Object> records = new ArrayList<>();
			records.add(parentId);
			req.setRecords(records);
			ctrl.replies(req, res);
			return res;
		}

		if (folder == null) {
			return res;
		}

		switch (folder) {
		case "archive":
			ctrl.archived(req, res);
			return res;
		case "important":
			ctrl.important(req, res);
			return res;
		default:
			ctrl.inbox(req, res);
			return res;
		}
	}

	@GET
	@Path("{id}/followers")
	public Response messageFollowers(@PathParam("id") long id) {

		@SuppressWarnings("all")
		final Repository<?> repo = JpaRepository.of((Class) getResource().getModel());
		final Model entity = repo.find(id);
		final Response response = new Response();

		final Object all = followers.findFollowers(entity);

		response.setData(all);
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}

	@POST
	@Path("{id}/follow")
	@SuppressWarnings("all")
	public Response messageFollow(@PathParam("id") long id, Request request) {

		final Repository<?> repo = JpaRepository.of((Class) getResource().getModel());
		final Model entity = repo.find(id);

		if (entity == null) {
			return messageFollowers(id);
		}
		if (request.getData() == null) {
			followers.follow(entity, AuthUtils.getUser());
			return messageFollowers(id);
		}

		final MailMessage message = Mapper.toBean(MailMessage.class, request.getData());
		if (message == null || message.getRecipients() == null || message.getRecipients().isEmpty()) {
			return messageFollowers(id);
		}

		for (MailAddress address : message.getRecipients()) {
			followers.follow(entity, address);
		}

		return messageFollowers(id);
	}

	@POST
	@Path("{id}/unfollow")
	@SuppressWarnings("all")
	public Response messageUnfollow(@PathParam("id") long id, Request request) {
		@SuppressWarnings("all")

		final Repository<?> repo = JpaRepository.of((Class) getResource().getModel());
		final Model entity = repo.find(id);
		if (entity == null) {
			return messageFollowers(id);
		}

		final List<Object> records = request.getRecords();
		if (records == null || records.isEmpty()) {
			followers.unfollow(entity, AuthUtils.getUser());
			return messageFollowers(id);
		}

		for (Object item  : records) {
			final MailFollower follower = followers.find(Longs.tryParse(item.toString()));
			if (follower != null) {
				followers.unfollow(follower);
			}
		}

		return messageFollowers(id);
	}

	@POST
	@Path("{id}/message")
	public Response messagePost(@PathParam("id") long id, Request request) {

		final Response response = new Response();
		@SuppressWarnings("all")
		final Repository<?> repo = JpaRepository.of((Class) getResource().getModel());
		final Context ctx = Context.create(request.getData(), MailMessage.class);
		final MailMessage msg = ctx.asType(MailMessage.class);

		final Model entity = repo.find(id);
		final List<?> ids = (List<?>) request.getData().get("files");
		List<MetaFile> files = null;

		if (ids != null && ids.size() > 0) {
			final MetaFileRepository repoFiles = Beans.get(MetaFileRepository.class);
			files = repoFiles.all().filter("self.id IN :ids").bind("ids", ids).fetch();
		}

		MailMessage saved = messages.post(entity, msg, files);
		Object item = messages.details(saved);

		response.setData(Lists.newArrayList(item));
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}
}