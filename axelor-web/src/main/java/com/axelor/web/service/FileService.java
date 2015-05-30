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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.axelor.common.FileUtils;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.schema.actions.ActionExport;
import com.axelor.meta.schema.actions.ActionReport;
import com.google.inject.persist.Transactional;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
@Path("/files")
public class FileService extends AbstractService {

	@Inject
	private MetaFiles files;

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
	@Path("report/{name:.*}")
	public javax.ws.rs.core.Response reportFile(@PathParam("name") final String name) {
		final File file = FileUtils.getFile(ActionReport.getOutputPath(), name);
		if (!file.isFile()) {
			throw new IllegalArgumentException(new FileNotFoundException(name));
		}

		MediaType type = MediaType.APPLICATION_OCTET_STREAM_TYPE;
		if (name.endsWith("pdf")) type = new MediaType("application", "pdf");
		if (name.endsWith("html")) type = new MediaType("text", "html");

		ResponseBuilder builder = javax.ws.rs.core.Response.ok(file, type);
		if (type != MediaType.APPLICATION_OCTET_STREAM_TYPE) {
			return builder.build();
		}

		return builder
				.header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
				.header("Content-Transfer-Encoding", "binary")
				.build();
	}

	@POST
	@Path("upload")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Transactional
	public javax.ws.rs.core.Response upload(
			@HeaderParam("X-File-Name") String fileName,
			@HeaderParam("X-File-Type") String fileType,
			@HeaderParam("X-File-Size") Long fileSize,
			InputStream stream) {

		final MetaFile bean = new MetaFile();

		if (fileSize == null) {
			fileSize = 0L;
		}

		bean.setFileName(fileName);
		bean.setMime(fileType);
		bean.setSize(fileSize);

		try {
			File file = Files.createTempFile(null, null).toFile();
			try {
				files.upload(file, bean);
			} finally {
				Files.deleteIfExists(file.toPath());
			}
		} catch (IOException e) {
			e.printStackTrace();
			return javax.ws.rs.core.Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		final File file = MetaFiles.getPath(bean).toFile();
		int total = 0;

		try(BufferedInputStream bis = new BufferedInputStream(stream);
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));) {
			int read = 0;
			byte[] bytes = new byte[2 * 8192];
			while ((read = bis.read(bytes)) != -1) {
				total += read;
				bos.write(bytes, 0, read);
			}
			//bos.flush();
			bean.setSize(Files.size(file.toPath()));
		} catch (Exception e) {
			try {
				files.delete(bean);
			} catch (Exception x) {}
			e.printStackTrace();
			return javax.ws.rs.core.Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		if (fileSize > 0 && total < fileSize) {
			try {
				files.delete(bean);
			} catch (Exception x) {}
			return javax.ws.rs.core.Response.status(Status.NOT_ACCEPTABLE).build();
		}

		return javax.ws.rs.core.Response.ok(bean).build();
	}
}
