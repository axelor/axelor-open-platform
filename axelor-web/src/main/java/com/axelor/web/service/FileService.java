/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.web.service;

import com.axelor.common.FileUtils;
import com.axelor.common.http.ContentDisposition;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.schema.actions.ActionExport;
import com.axelor.meta.schema.actions.validate.ActionValidateBuilder;
import com.axelor.meta.schema.actions.validate.validator.ValidatorType;
import com.google.inject.persist.Transactional;
import com.google.inject.servlet.RequestScoped;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "DMS")
public class FileService extends AbstractService {

  @Inject private MetaFiles files;

  @GET
  @Path("data-export/{name:.*}")
  @Hidden
  public javax.ws.rs.core.Response exportFile(@PathParam("name") final String name) {
    final File file = FileUtils.getFile(ActionExport.getExportPath(), name);
    if (!file.isFile()) {
      return javax.ws.rs.core.Response.status(Status.NOT_FOUND).build();
    }
    return javax.ws.rs.core.Response.ok(file, MediaType.APPLICATION_OCTET_STREAM_TYPE)
        .header(
            "Content-Disposition",
            ContentDisposition.attachment().filename(file.getName()).build().toString())
        .header("Content-Transfer-Encoding", "binary")
        .build();
  }

  @GET
  @Path("report/{link:.*}")
  @Hidden
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
      return builder
          .header(
              "Content-Disposition",
              ContentDisposition.inline().filename(fileName).build().toString())
          .build();
    }

    return builder
        .header(
            "Content-Disposition",
            ContentDisposition.attachment().filename(fileName).build().toString())
        .header("Content-Transfer-Encoding", "binary")
        .build();
  }

  @DELETE
  @Path("upload/{fileId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Hidden
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
  @Operation(
      summary = "File upload",
      description =
          "The upload service doesn’t include file to DMS directly, but creates MetaFile records pointing to the uploaded file. The MetaFile record can be used later to create a DMSFile record (normal or attachment).")
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
      final String safeFileName = FileUtils.safeFileName(fileName);

      // check if file name is valid
      MetaFiles.checkPath(safeFileName);
      MetaFiles.checkType(fileType);
      final File file = files.upload(stream, fileOffset, fileSize, fileId);
      // check if file content is valid
      MetaFiles.checkType(file);
      if (Files.size(file.toPath()) == fileSize) {
        final MetaFile meta = new MetaFile();
        meta.setFileName(safeFileName);
        meta.setFileType(fileType);
        files.upload(file, meta);
        // Keep original file name
        meta.setFileName(fileName);
        return javax.ws.rs.core.Response.ok(meta).build();
      }
    } catch (IllegalArgumentException e) {
      ActionValidateBuilder validateBuilder =
          new ActionValidateBuilder(ValidatorType.ERROR).setMessage(e.getMessage());
      data.putAll(validateBuilder.build());
      return javax.ws.rs.core.Response.status(Status.BAD_REQUEST).entity(data).build();
    } catch (Exception e) {
      LOG.error("Error when uploading file:", e);
      ActionValidateBuilder validateBuilder =
          new ActionValidateBuilder(ValidatorType.ERROR).setMessage(e.getMessage());
      data.putAll(validateBuilder.build());
      return javax.ws.rs.core.Response.status(Status.INTERNAL_SERVER_ERROR).entity(data).build();
    }

    if (fileOffset == 0L) {
      data.put("fileId", fileId);
    }

    return javax.ws.rs.core.Response.ok(data).build();
  }
}
