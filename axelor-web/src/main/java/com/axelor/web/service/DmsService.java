/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.dms.db.DMSFile;
import com.axelor.dms.db.repo.DMSFileRepository;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaFileRepository;
import com.axelor.rpc.Request;
import com.axelor.rpc.Resource;
import com.axelor.rpc.Response;
import com.axelor.script.NashornScriptHelper;
import com.axelor.script.ScriptHelper;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Longs;
import com.google.inject.servlet.RequestScoped;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.api.scripting.ScriptUtils;
import org.eclipse.persistence.annotations.Transformation;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/dms")
@RequestScoped
public class DmsService {

  @Context private HttpServletRequest httpRequest;

  @Inject private DMSFileRepository repository;

  private static final Map<String, String> EXTS =
      ImmutableMap.of("html", ".html", "spreadsheet", ".csv");

  @GET
  @Path("files")
  public Response listFiles(
      @QueryParam("parent") Long parentId, @QueryParam("pattern") String pattern) {
    final Response response = new Response();
    final StringBuilder filter = new StringBuilder("self.parent");

    if (parentId == null || parentId <= 0) {
      filter.append(" is null");
    } else {
      filter.append(" = :parent");
    }
    if (!StringUtils.isBlank(pattern)) {
      pattern = "%" + pattern + "%";
      filter.append(" AND UPPER(self.fileName) like UPPER(:pattern)");
    }

    final Query<?> query =
        repository
            .all()
            .filter(filter.toString())
            .bind("parent", parentId)
            .bind("pattern", pattern);

    final Long count = query.count();
    final List<?> records = query.select("fileName", "isDirectory").fetch(-1, -1);

    response.setStatus(Response.STATUS_SUCCESS);
    response.setData(records);
    response.setTotal(count);
    return response;
  }

  @GET
  @Path("attachments/{model}/{id}")
  public Response attachments(@PathParam("model") String model, @PathParam("id") Long id) {
    final Response response = new Response();
    final List<?> records =
        repository
            .all()
            .filter(
                "self.relatedId = :id AND self.relatedModel = :model AND self.metaFile is not null AND self.isDirectory = false")
            .bind("id", id)
            .bind("model", model)
            .select("fileName")
            .fetch(-1, -1);
    response.setStatus(Response.STATUS_SUCCESS);
    response.setData(records);
    response.setTotal(records.size());
    return response;
  }

  @PUT
  @Path("attachments/{model}/{id}")
  @Transformation
  public Response addAttachments(
      @PathParam("model") String model, @PathParam("id") Long id, Request request) {
    if (request == null || ObjectUtils.isEmpty(request.getRecords())) {
      throw new IllegalArgumentException("No attachment records provided.");
    }
    final Class<?> modelClass;
    try {
      modelClass = Class.forName(model);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("No such model found.");
    }
    final Object entity = JPA.em().find(modelClass, id);
    if (!(entity instanceof Model)) {
      throw new IllegalArgumentException("No such record found.");
    }

    final MetaFileRepository filesRepo = Beans.get(MetaFileRepository.class);
    final List<MetaFile> items = new ArrayList<>();

    for (Object item : request.getRecords()) {
      @SuppressWarnings("rawtypes")
      Object fileRecord = filesRepo.find(Longs.tryParse(((Map) item).get("id").toString()));
      if (fileRecord instanceof MetaFile) {
        items.add((MetaFile) fileRecord);
      } else {
        throw new IllegalArgumentException("Invalid list of attachment records.");
      }
    }

    final MetaFiles files = Beans.get(MetaFiles.class);
    final Response response = new Response();
    final List<Object> records = new ArrayList<>();

    for (MetaFile file : items) {
      DMSFile dmsFile = files.attach(file, file.getFileName(), (Model) entity);
      records.add(Resource.toMapCompact(dmsFile));
    }

    response.setStatus(Response.STATUS_SUCCESS);
    response.setData(records);
    return response;
  }

  @GET
  @Path("offline")
  public Response getOfflineFiles(
      @QueryParam("limit") int limit, @QueryParam("offset") int offset) {

    final Response response = new Response();
    final List<DMSFile> files = repository.findOffline(limit, offset);
    final long count =
        repository
            .all()
            .filter("self.permissions[].value = 'OFFLINE' AND self.permissions[].user = :user")
            .bind("user", AuthUtils.getUser())
            .count();

    final List<Object> data = new ArrayList<>();
    for (DMSFile file : files) {
      final Map<String, Object> json = Resource.toMap(file, "fileName");
      final MetaFile metaFile = file.getMetaFile();
      LocalDateTime lastModified = file.getUpdatedOn();
      if (metaFile != null) {
        lastModified = metaFile.getCreatedOn();
        json.put("fileSize", metaFile.getFileSize());
        json.put("fileType", metaFile.getFileType());
      }

      json.put("id", file.getId());
      json.put("updatedOn", lastModified);

      data.add(json);
    }

    response.setData(data);
    response.setOffset(offset);
    response.setTotal(count);
    response.setStatus(Response.STATUS_SUCCESS);

    return response;
  }

  @POST
  @Path("offline")
  public Response offline(Request request) {
    final Response response = new Response();
    final List<?> ids = request.getRecords();

    if (ids == null || ids.isEmpty()) {
      response.setStatus(Response.STATUS_SUCCESS);
      return response;
    }

    final List<DMSFile> records =
        repository.all().filter("self.id in :ids").bind("ids", ids).fetch();

    boolean unset;
    try {
      unset = "true".equals(request.getData().get("unset").toString());
    } catch (Exception e) {
      unset = false;
    }

    for (DMSFile item : records) {
      repository.setOffline(item, unset);
    }

    response.setStatus(Response.STATUS_SUCCESS);
    return response;
  }

  private File findFile(DMSFile file) {
    if (file == null || file.getMetaFile() == null) {
      return null;
    }
    final File path = MetaFiles.getPath(file.getMetaFile()).toFile();
    return path.exists() ? path : null;
  }

  @HEAD
  @Path("offline/{id}")
  public javax.ws.rs.core.Response doDownloadCheck(@PathParam("id") long id) {
    final DMSFile file = repository.find(id);
    return findFile(file) == null
        ? javax.ws.rs.core.Response.status(Status.NOT_FOUND).build()
        : javax.ws.rs.core.Response.ok().build();
  }

  @GET
  @Path("offline/{id}")
  public javax.ws.rs.core.Response doDownload(@PathParam("id") long id) {

    final DMSFile file = repository.find(id);
    final File path = findFile(file);
    if (path == null) {
      return javax.ws.rs.core.Response.status(Status.NOT_FOUND).build();
    }

    final StreamingOutput so =
        new StreamingOutput() {
          @Override
          public void write(OutputStream output) throws IOException, WebApplicationException {
            try (InputStream input = new FileInputStream(path)) {
              writeTo(output, input);
            }
          }
        };

    return stream(so, file.getFileName(), false);
  }

  @POST
  @Path("download/batch")
  public javax.ws.rs.core.Response onDownload(Request request) {

    final List<Object> ids = request.getRecords();

    if (ids == null || ids.isEmpty()) {
      return javax.ws.rs.core.Response.status(Status.NOT_FOUND).build();
    }

    final List<DMSFile> records =
        repository.all().filter("self.id in :ids").bind("ids", ids).fetch();

    if (records.size() != ids.size()) {
      return javax.ws.rs.core.Response.status(Status.NOT_FOUND).build();
    }

    final String batchId = UUID.randomUUID().toString();
    final Map<String, Object> data = new HashMap<>();

    String batchName = "documents-" + LocalDate.now() + ".zip";
    if (records.size() == 1) {
      batchName = records.get(0).getFileName();
    }

    data.put("batchId", batchId);
    data.put("batchName", batchName);

    httpRequest.getSession().setAttribute(batchId, ids);

    return javax.ws.rs.core.Response.ok(data).build();
  }

  private List<?> findBatchIds(String batchOrId) {
    List<?> ids = (List<?>) httpRequest.getSession().getAttribute(batchOrId);
    if (ids == null) {
      Long id = Longs.tryParse(batchOrId);
      ids = id == null ? null : Arrays.asList(id);
    }

    if (ids == null || ids.isEmpty()) {
      return null;
    }
    return ids;
  }

  @HEAD
  @Path("download/{id}")
  public javax.ws.rs.core.Response doDownloadCheck(@PathParam("id") String batchOrId) {
    return findBatchIds(batchOrId) == null
        ? javax.ws.rs.core.Response.status(Status.NOT_FOUND).build()
        : javax.ws.rs.core.Response.ok().build();
  }

  @GET
  @Path("download/{id}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public javax.ws.rs.core.Response doDownload(@PathParam("id") String batchOrId) {
    return getAttachmentResponse(batchOrId, false);
  }

  @GET
  @Path("inline/{id}")
  public javax.ws.rs.core.Response doInline(@PathParam("id") String batchOrId) {
    return getAttachmentResponse(batchOrId, true);
  }

  private javax.ws.rs.core.Response getAttachmentResponse(String batchOrId, boolean inline) {
    final List<?> ids = findBatchIds(batchOrId);
    if (ids == null) {
      return javax.ws.rs.core.Response.status(Status.NOT_FOUND).build();
    }

    final List<DMSFile> records =
        repository.all().filter("self.id in :ids").bind("ids", ids).fetch();

    if (records.size() != ids.size()) {
      return javax.ws.rs.core.Response.status(Status.NOT_FOUND).build();
    }

    // if file
    if (records.size() == 1) {
      final DMSFile record = records.get(0);
      File file = getFile(record);
      if (file != null) {
        return stream(file, getFileName(record), inline);
      }
    }

    final StreamingOutput so =
        new StreamingOutput() {
          @Override
          public void write(OutputStream output) throws IOException, WebApplicationException {
            final ZipOutputStream zos = new ZipOutputStream(output);
            try {
              for (DMSFile file : records) {
                writeToZip(zos, file);
              }
            } finally {
              zos.close();
            }
          }
        };

    final String batchName = "documents-" + LocalDate.now() + ".zip";
    return stream(so, batchName, inline);
  }

  private File getFile(DMSFile record) {
    if (record.getMetaFile() != null) {
      MetaFile file = record.getMetaFile();
      return MetaFiles.getPath(file).toFile();
    }

    if (StringUtils.isBlank(record.getContentType())) {
      return null;
    }

    try {
      switch (record.getContentType()) {
        case "html":
          {
            final java.nio.file.Path path = MetaFiles.createTempFile(record.getFileName(), ".html");
            final File file = path.toFile();
            if (StringUtils.notBlank(record.getContent())) {
              try (final FileWriter writer = new FileWriter(file)) {
                writer.append(record.getContent());
              }
            }
            return file;
          }
        case "spreadsheet":
          {
            final java.nio.file.Path path = MetaFiles.createTempFile(record.getFileName(), ".csv");
            final File file = path.toFile();
            final ScriptHelper scriptHelper = new NashornScriptHelper(null);
            try (final PrintStream writer = new PrintStream(file)) {
              if (StringUtils.notBlank(record.getContent())) {
                final ScriptObjectMirror content =
                    (ScriptObjectMirror) scriptHelper.eval(record.getContent());
                if (content != null) {
                  for (final Object value : content.values()) {
                    final Object line = ScriptUtils.convert(value, String.class);
                    writer.println(line != null ? line.toString() : "");
                  }
                }
              }
            }
            return file;
          }
        default:
          throw new IllegalArgumentException("Unsupported content type");
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String getFileName(DMSFile record) {
    return record.getFileName() + EXTS.getOrDefault(record.getContentType(), "");
  }

  private Map<String, File> findFiles(DMSFile file, String base) {
    final User user = AuthUtils.getUser();

    if (user == null) {
      return Collections.emptyMap();
    }

    String childrenQlString = "self.parent = :parent";

    if (!AuthUtils.isAdmin(user)) {
      childrenQlString += " AND (self.permissions.user = :user OR self.permissions.group = :group)";
    }

    return findFiles(file, base, childrenQlString, user);
  }

  private Map<String, File> findFiles(
      DMSFile file, String base, String childrenQlString, User user) {
    final Map<String, File> files = new LinkedHashMap<>();
    if (file.getIsDirectory() == Boolean.TRUE) {
      final List<DMSFile> children =
          repository
              .all()
              .filter(childrenQlString, file, user, user.getGroup())
              .bind("parent", file)
              .bind("user", user)
              .bind("group", user.getGroup())
              .fetch();
      final String path = base + "/" + file.getFileName();
      files.put(path + "/", null);
      for (DMSFile child : children) {
        files.putAll(findFiles(child, path, childrenQlString, user));
      }
      return files;
    }
    final File relatedFile = getFile(file);
    if (relatedFile != null) {
      files.put(base + "/" + getFileName(file), relatedFile);
    }
    return files;
  }

  private void writeToZip(ZipOutputStream zos, DMSFile dmsFile) throws IOException {
    final Map<String, File> files = findFiles(dmsFile, "");
    for (final String entry : files.keySet()) {
      File file = files.get(entry);
      zos.putNextEntry(new ZipEntry(entry.charAt(0) == '/' ? entry.substring(1) : entry));
      if (file == null) {
        zos.closeEntry();
        continue;
      }
      final FileInputStream fis = new FileInputStream(file);
      try {
        writeTo(zos, fis);
      } finally {
        fis.close();
        zos.closeEntry();
      }
    }
  }

  private void writeTo(OutputStream os, InputStream is) throws IOException {
    int read = 0;
    byte[] bytes = new byte[2048];
    while ((read = is.read(bytes)) != -1) {
      os.write(bytes, 0, read);
    }
  }

  private javax.ws.rs.core.Response stream(Object content, String fileName, boolean inline) {
    MediaType type = MediaType.APPLICATION_OCTET_STREAM_TYPE;

    if (inline) {
      if (fileName.endsWith(".pdf")) type = new MediaType("application", "pdf");
      if (fileName.endsWith(".html")) type = new MediaType("text", "html");
      if (fileName.endsWith(".png")) type = new MediaType("image", "png");
      if (fileName.endsWith(".jpg")) type = new MediaType("image", "jpg");
      if (fileName.endsWith(".svg")) type = new MediaType("image", "svg+xml");
      if (fileName.endsWith(".gif")) type = new MediaType("image", "gif");
      if (fileName.endsWith(".webp")) type = new MediaType("image", "webp");
    }

    final ResponseBuilder builder = javax.ws.rs.core.Response.ok(content, type);

    if (inline && type != MediaType.APPLICATION_OCTET_STREAM_TYPE) {
      return builder.header("Content-Disposition", "inline; filename=\"" + fileName + "\"").build();
    }

    return builder
        .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
        .header("Content-Transfer-Encoding", "binary")
        .build();
  }
}
