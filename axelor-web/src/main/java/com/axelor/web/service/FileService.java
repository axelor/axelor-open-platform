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

import com.axelor.common.FileUtils;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.schema.actions.ActionExport;
import com.google.inject.persist.Transactional;
import com.google.inject.servlet.RequestScoped;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

@RequestScoped
@Path("/files")
public class FileService extends AbstractService {

  @Inject private MetaFiles files;

  @GET
  @Path("data-export/{name:.*}")
  public javax.ws.rs.core.Response exportFile(@PathParam("name") final String name) {
    final File file = FileUtils.getFile(ActionExport.getExportPath(), name);
    if (!file.isFile()) {
      throw new IllegalArgumentException(new FileNotFoundException(name));
    }
    return javax.ws.rs.core.Response.ok(file, MediaType.APPLICATION_OCTET_STREAM_TYPE)
        .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
        .header("Content-Transfer-Encoding", "binary")
        .build();
  }

  @GET
  @Path("report/{link:.*}")
  public javax.ws.rs.core.Response reportFile(
      @PathParam("link") final String link, @QueryParam("name") final String name) {

    final java.nio.file.Path file = MetaFiles.findTempFile(link);
    if (file == null || !file.toFile().isFile()) {
      throw new IllegalArgumentException(new FileNotFoundException(name));
    }

    MediaType type = MediaType.APPLICATION_OCTET_STREAM_TYPE;
    if (name.endsWith(".pdf")) type = new MediaType("application", "pdf");
    if (name.endsWith(".html")) type = new MediaType("text", "html");
    if (name.endsWith(".png")) type = new MediaType("image", "png");
    if (name.endsWith(".jpg")) type = new MediaType("image", "jpg");
    if (name.endsWith(".svg")) type = new MediaType("image", "svg+xml");
    if (name.endsWith(".gif")) type = new MediaType("image", "gif");
    if (name.endsWith(".webp")) type = new MediaType("image", "webp");

    final String fileName = name == null ? file.toFile().getName() : name;
    final ResponseBuilder builder = javax.ws.rs.core.Response.ok(file.toFile(), type);

    if (type != MediaType.APPLICATION_OCTET_STREAM_TYPE) {
      return builder.header("Content-Disposition", "inline; filename=\"" + fileName + "\"").build();
    }

    return builder
        .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
        .header("Content-Transfer-Encoding", "binary")
        .build();
  }

  @DELETE
  @Path("upload/{fileId}")
  @Produces(MediaType.APPLICATION_JSON)
  public javax.ws.rs.core.Response clean(@PathParam("fileId") String fileId) {
    try {
      files.clean(fileId);
    } catch (IOException e) {
    }
    return javax.ws.rs.core.Response.ok().build();
  }

  @POST
  @Path("upload")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Transactional
  public javax.ws.rs.core.Response upload(
      @HeaderParam("X-File-Id") String fileId,
      @HeaderParam("X-File-Name") String fileName,
      @HeaderParam("X-File-Type") String fileType,
      @HeaderParam("X-File-Size") Long fileSize,
      @HeaderParam("X-File-Offset") Long fileOffset,
      InputStream stream) {

    if (fileName == null || fileSize == null || fileOffset == null) {
      return javax.ws.rs.core.Response.status(Status.BAD_REQUEST).build();
    }
    if (fileId == null && fileOffset == 0L) {
      fileId = UUID.randomUUID().toString();
    }
    if (fileId == null) {
      return javax.ws.rs.core.Response.status(Status.BAD_REQUEST).build();
    }

    final Map<String, Object> data = new HashMap<>();
    try {
      fileName = URLDecoder.decode(fileName, "UTF-8");

      // check if file name is valid
      MetaFiles.checkPath(fileName);
      MetaFiles.checkType(fileType);
      final File file = files.upload(stream, fileOffset, fileSize, fileId);
      // check if file content is valid
      MetaFiles.checkType(file);
      if (Files.size(file.toPath()) == fileSize) {
        final MetaFile meta = new MetaFile();
        meta.setFileName(fileName);
        meta.setFileType(fileType);
        files.upload(file, meta);
        return javax.ws.rs.core.Response.ok(meta).build();
      }
    } catch (IllegalArgumentException e) {
      data.put("error", e.getMessage());
      return javax.ws.rs.core.Response.status(Status.BAD_REQUEST).entity(data).build();
    } catch (Exception e) {
      e.printStackTrace();
      data.put("error", e.getMessage());
      return javax.ws.rs.core.Response.status(Status.INTERNAL_SERVER_ERROR).entity(data).build();
    }

    if (fileOffset == 0L) {
      data.put("fileId", fileId);
    }

    return javax.ws.rs.core.Response.ok(data).build();
  }
}
