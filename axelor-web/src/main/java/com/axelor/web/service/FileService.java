/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.service;

import com.axelor.common.FileUtils;
import com.axelor.common.MimeTypesUtils;
import com.axelor.common.StringUtils;
import com.axelor.common.http.ContentDisposition;
import com.axelor.file.temp.TempFiles;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.schema.actions.validate.ActionValidateBuilder;
import com.axelor.meta.schema.actions.validate.validator.ValidatorType;
import com.axelor.rpc.PendingExportService;
import com.google.inject.persist.Transactional;
import com.google.inject.servlet.RequestScoped;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequestScoped
@Path("/files")
@Tag(name = "DMS")
public class FileService extends AbstractService {

  private final MetaFiles files;
  private final PendingExportService pendingExportService;

  @Inject
  public FileService(MetaFiles files, PendingExportService pendingExportService) {
    this.files = files;
    this.pendingExportService = pendingExportService;
  }

  @HEAD
  @Path("data-export")
  @Hidden
  public jakarta.ws.rs.core.Response checkExportFile(
      @QueryParam("token") final String token, @QueryParam("fileName") final String name) {
    if (StringUtils.isBlank(token)) {
      return jakarta.ws.rs.core.Response.status(Status.BAD_REQUEST).build();
    }

    var export = pendingExportService.get(token);

    if (export == null) {
      return jakarta.ws.rs.core.Response.status(Status.NOT_FOUND).build();
    }

    var fileName = StringUtils.isBlank(name) ? export.getFileName().toString() : name;

    return createExportResponseBuilder(fileName).build();
  }

  @GET
  @Path("data-export")
  @Hidden
  public jakarta.ws.rs.core.Response downloadExportFile(
      @QueryParam("token") final String token, @QueryParam("fileName") final String name) {
    if (StringUtils.isBlank(token)) {
      return jakarta.ws.rs.core.Response.status(Status.BAD_REQUEST).build();
    }

    var export = pendingExportService.remove(token);

    if (export == null) {
      return jakarta.ws.rs.core.Response.status(Status.NOT_FOUND).build();
    }

    StreamingOutput stream =
        output -> {
          try (var is = Files.newInputStream(export)) {
            is.transferTo(output);
            output.flush();
          } finally {
            Files.deleteIfExists(export);
          }
        };

    var fileName = StringUtils.isBlank(name) ? export.getFileName().toString() : name;

    return createExportResponseBuilder(fileName)
        .entity(stream)
        .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
        .build();
  }

  private ResponseBuilder createExportResponseBuilder(String fileName) {
    return jakarta.ws.rs.core.Response.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(fileName).build().toString())
        .header("Content-Transfer-Encoding", "binary");
  }

  @GET
  @Path("report")
  @Hidden
  public jakarta.ws.rs.core.Response downloadReportFile(
      @QueryParam("link") final String link, @QueryParam("name") final String name) {
    if (StringUtils.isBlank(link)) {
      return jakarta.ws.rs.core.Response.status(Status.BAD_REQUEST).build();
    }

    final java.nio.file.Path file = TempFiles.findTempFile(link);
    if (file == null || !file.toFile().isFile()) {
      throw new IllegalArgumentException(new FileNotFoundException(name));
    }

    final MediaType type = MediaType.valueOf(MimeTypesUtils.getContentType(file));
    final String fileName = name == null ? file.toFile().getName() : name;
    final ResponseBuilder builder = jakarta.ws.rs.core.Response.ok(file.toFile(), type);

    if (MetaFiles.isBrowserPreviewCompatible(type)) {
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
  public jakarta.ws.rs.core.Response clean(@PathParam("fileId") String fileId) {
    try {
      TempFiles.clean(fileId);
    } catch (IOException e) {
    }
    return jakarta.ws.rs.core.Response.ok().build();
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
  public jakarta.ws.rs.core.Response upload(
      @HeaderParam("X-File-Id") String fileId,
      @HeaderParam("X-File-Name") String fileName,
      @HeaderParam("X-File-Type") String fileType,
      @HeaderParam("X-File-Size") Long fileSize,
      @HeaderParam("X-File-Offset") Long fileOffset,
      InputStream stream) {

    if (fileName == null || fileSize == null || fileOffset == null) {
      return jakarta.ws.rs.core.Response.status(Status.BAD_REQUEST).build();
    }
    if (fileId == null && fileOffset == 0L) {
      fileId = UUID.randomUUID().toString();
    }
    if (fileId == null) {
      return jakarta.ws.rs.core.Response.status(Status.BAD_REQUEST).build();
    }

    final Map<String, Object> data = new HashMap<>();
    try {
      fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
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
        return jakarta.ws.rs.core.Response.ok(meta).build();
      }
    } catch (IllegalArgumentException e) {
      ActionValidateBuilder validateBuilder =
          new ActionValidateBuilder(ValidatorType.ERROR).setMessage(e.getMessage());
      data.putAll(validateBuilder.build());
      return jakarta.ws.rs.core.Response.status(Status.BAD_REQUEST).entity(data).build();
    } catch (Exception e) {
      LOG.error("Error when uploading file:", e);
      ActionValidateBuilder validateBuilder =
          new ActionValidateBuilder(ValidatorType.ERROR).setMessage(e.getMessage());
      data.putAll(validateBuilder.build());
      return jakarta.ws.rs.core.Response.status(Status.INTERNAL_SERVER_ERROR).entity(data).build();
    }

    if (fileOffset == 0L) {
      data.put("fileId", fileId);
    }

    return jakarta.ws.rs.core.Response.ok(data).build();
  }
}
