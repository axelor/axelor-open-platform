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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.joda.time.LocalDate;

import com.axelor.dms.db.DMSFile;
import com.axelor.dms.db.repo.DMSFileRepository;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.rpc.Request;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
@Path("/dms")
public class DmsService {

	@Context
	private HttpServletRequest httpRequest;

	@Inject
	private DMSFileRepository repository;

	@POST
	@Path("download/batch")
	public javax.ws.rs.core.Response onDownload(Request request) {

		final List<Object> ids = request.getRecords();

		if (ids == null ||  ids.isEmpty()) {
			return javax.ws.rs.core.Response.status(Status.NOT_FOUND).build();
		}

		final List<DMSFile> records = repository.all()
				.filter("self.id in :ids")
				.bind("ids", ids)
				.fetch();

		if (records.size() != ids.size()) {
			return javax.ws.rs.core.Response.status(Status.NOT_FOUND).build();
		}

		final String batchId = UUID.randomUUID().toString();
		final Map<String, Object> data = new HashMap<>();

		String batchName = "documents-" + new LocalDate().toString("yyyy-MM-dd") + ".zip";
		if (records.size() == 1) {
			batchName = records.get(0).getFileName();
		}

		data.put("batchId", batchId);
		data.put("batchName", batchName);

		httpRequest.getSession().setAttribute(batchId, ids);

		return javax.ws.rs.core.Response.ok(data).build();
	}

	@GET
	@Path("download/{batchId}")
	public javax.ws.rs.core.Response doDownload(@PathParam("batchId") String batchId) {

		@SuppressWarnings("all")
		final List<Object> ids = (List) httpRequest.getSession().getAttribute(batchId);
		if (ids == null) {
			return javax.ws.rs.core.Response.status(Status.NOT_FOUND).build();
		}

		final List<DMSFile> records = repository.all()
				.filter("self.id in :ids")
				.bind("ids", ids)
				.fetch();

		if (records.size() != ids.size()) {
			return javax.ws.rs.core.Response.status(Status.NOT_FOUND).build();
		}

		// if file
		if (records.size() == 1 && records.get(0).getMetaFile() != null) {
			MetaFile file = records.get(0).getMetaFile();
			return stream(MetaFiles.getPath(file).toFile(), file.getFileName());
		}

		final StreamingOutput so = new StreamingOutput() {
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

		final String batchName = "documents-" + new LocalDate().toString("yyyy-MM-dd") + ".zip";
		return stream(so, batchName);
	}

	private Map<String, File> findFiles(DMSFile file, String base) {
		final Map<String, File> files = new LinkedHashMap<>();
		if (file.getIsDirectory() == Boolean.TRUE) {
			final List<DMSFile> children = repository.all()
					.filter("self.parent = ?", file)
					.fetch();
			final String path = base + "/" + file.getFileName();
			files.put(path + "/", null);
			for (DMSFile child : children) {
				files.putAll(findFiles(child, path));
			}
			return files;
		}
		if (file.getMetaFile() != null) {
			files.put(base + "/" + file.getFileName(), MetaFiles.getPath(file.getMetaFile()).toFile());
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
			final BufferedInputStream bis = new BufferedInputStream(fis);
			try {
				int read = 0;
				byte[] bytes = new byte[2048];
				while ((read = bis.read(bytes)) != -1) {
					zos.write(bytes, 0, read);
				}
			} finally {
				bis.close();
				zos.closeEntry();
			}
		}
	}

	private javax.ws.rs.core.Response stream(Object content, String fileName) {
		return javax.ws.rs.core.Response
				.ok(content, MediaType.APPLICATION_OCTET_STREAM_TYPE)
				.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
				.header("Content-Transfer-Encoding", "binary").build();
	}
}
