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
package com.axelor.meta.schema.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.joda.time.DateTime;

import com.axelor.db.JPA;
import com.axelor.dms.db.DMSFile;
import com.axelor.dms.db.repo.DMSFileRepository;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.report.ReportGenerator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Throwables;

public class ActionReport extends Action {

	private static final String DEFAULT_FORMAT = "pdf";

	@XmlType
	public static class Parameter extends ActionRecord.RecordField {

	}

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

	@XmlElement(name = "param")
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

		final Path tmp1 = MetaFiles.createTempFile(null, "");
		final ReportGenerator generator = Beans.get(ReportGenerator.class);

		try(FileOutputStream stream = new FileOutputStream(tmp1.toFile())) {
			generator.generate(stream, designName, format, params);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}

		return tmp1.toFile();
	}

	private Object attach(File tempFile, String name, Class<?> model, Long id) throws IOException {

		final MetaFiles files = Beans.get(MetaFiles.class);
		final MetaFile metaFile = new MetaFile();

		metaFile.setFileName(name);

		String mime = java.nio.file.Files.probeContentType(tempFile.toPath());
		if (mime == null) {
			if (format.equals("pdf")) mime = "application/pdf";
			if (format.equals("doc")) mime = "application/msword";
			if (format.equals("ps")) mime = "application/postscript";
			if (format.equals("html")) mime = "text/html";
		}

		metaFile.setFileType(mime);

		files.upload(tempFile, metaFile);

		final DMSFile dmsFile = new DMSFile();
		dmsFile.setFileName(name);
		dmsFile.setMetaFile(metaFile);
		dmsFile.setRelatedId(id);
		dmsFile.setRelatedModel(model.getName());

		final DMSFileRepository repository = Beans.get(DMSFileRepository.class);
		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {
				repository.save(dmsFile);
			}
		});

		return dmsFile;
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
