/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
package com.axelor.meta.schema.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.joda.time.DateTime;

import com.axelor.app.AppSettings;
import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaAttachmentRepository;
import com.axelor.report.ReportGenerator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Throwables;
import com.google.common.io.Files;

public class ActionReport extends Action {

	@XmlType
	public static class Parameter extends ActionRecord.RecordField {

	}

	private static final String DEFAULT_UPLOAD_DIR = "{java.io.tmpdir}/axelor/attachments";
	private static final String DEFAULT_TEMP_DIR = "{java.io.tmpdir}/axelor/reports-gen";
	private static final String DEFAULT_FORMAT = "pdf";

	private static final Path UPLOAD_DIR = Paths.get(AppSettings.get().getPath("file.upload.dir", DEFAULT_UPLOAD_DIR));
	private static final Path TEMP_DIR = Paths.get(AppSettings.get().getPath("reports.output.dir", DEFAULT_TEMP_DIR));

	@XmlAttribute
	private String title;

	@XmlAttribute(name = "design")
	private String designName;

	@XmlAttribute(name = "output")
	private String outputName;

	@XmlAttribute
	private String format = DEFAULT_FORMAT;

	@XmlAttribute
	private Boolean attachment;

	@XmlElement(name = "parameter")
	private List<Parameter> parameters;

	@JsonGetter("title")
	public String getLocalizedTitle() {
		return I18n.get(title);
	}

	@JsonIgnore
	public String getTitle() {
		return title;
	}

	public String getDesignName() {
		return designName;
	}

	public String getOutputName() {
		return outputName;
	}

	public String getFormat() {
		return format;
	}

	public Boolean getAttachment() {
		return attachment;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public static File getOutputPath() {
		return TEMP_DIR.toFile();
	}

	private File render(ActionHandler handler, String fileName) throws IOException {

		Map<String, Object> params = new HashMap<>();
		if (parameters != null) {
			for (Parameter param : parameters) {
				if (param.test(handler)) {
					params.put(param.getName(), handler.evaluate(param.getExpression()));
				}
			}
		}

		log.debug("with params: {}", params);

		final File output = TEMP_DIR.resolve(fileName).toFile();
		final ReportGenerator generator = Beans.get(ReportGenerator.class);

		Files.createParentDirs(output);

		try(FileOutputStream stream = new FileOutputStream(output)) {
			generator.generate(stream, designName, format, params);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}

		return output;
	}

	private Object attach(File tempFile, String name, Class<?> model, Long id) throws IOException {

		Path relative = Paths.get("reports", tempFile.getName());
		Path target = UPLOAD_DIR.resolve(relative);


		Files.createParentDirs(target.toFile());
		Files.move(tempFile, target.toFile());

		final MetaFile metaFile = new MetaFile();
		metaFile.setFileName(name);
		metaFile.setFilePath(relative.toString());
		metaFile.setSize(tempFile.length());

		String mime = java.nio.file.Files.probeContentType(tempFile.toPath());
		if (mime == null) {
			if (format.equals("pdf")) mime = "application/pdf";
			if (format.equals("doc")) mime = "application/msword";
			if (format.equals("ps")) mime = "application/postscript";
			if (format.equals("html")) mime = "text/html";
		}

		metaFile.setMime(mime);

		final MetaAttachment metaAttachment = new MetaAttachment();
		metaAttachment.setObjectName(model.getName());
		metaAttachment.setObjectId(id);
		metaAttachment.setMetaFile(metaFile);

		final MetaAttachmentRepository repository = Beans.get(MetaAttachmentRepository.class);
		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {
				repository.save(metaAttachment);
			}
		});

		return metaAttachment;
	}

	private Object _evaluate(ActionHandler handler) throws IOException {
		log.debug("action-report: {}", getName());

		final Map<String, Object> result = new HashMap<>();
		final Class<?> klass = handler.getContext().getContextClass();
		final Long id = (Long) handler.getContext().get("id");

		final String outputName = this.outputName
				.replace("${date}", new DateTime().toString("yyyyMMdd"))
				.replace("${time}", new DateTime().toString("HHmmss"))
				.replace("${name}", getName());

		final Long rnd = Math.abs(UUID.randomUUID().getMostSignificantBits());
		final String tempName = String.format("%s-%s.%s", outputName, rnd, format);
		final String fileName = String.format("%s.%s", outputName, format);
		final File output = render(handler, tempName);

		result.put("report", getName());
		result.put("reportFile", fileName);
		result.put("reportLink", output.getName());
		result.put("reportFormat", format);

		if (attachment == Boolean.TRUE && id != null) {
			result.put("attached", attach(output, fileName, klass, id));
		}

		return result;
	}

	@Override
	public Object evaluate(ActionHandler handler) {
		try {
			return _evaluate(handler);
		} catch (Exception e) {
			e.printStackTrace();
			throw Throwables.propagate(e);
		}
	}

	@Override
	public Object wrap(ActionHandler handler) {
		return evaluate(handler);
	}
}
