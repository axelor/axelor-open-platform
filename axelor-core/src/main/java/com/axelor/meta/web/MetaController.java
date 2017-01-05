/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.meta.web;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.axelor.app.AppSettings;
import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.axelor.i18n.I18nBundle;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaField;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.MetaTranslation;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaTranslationRepository;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Inject;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class MetaController {

	@Inject
	private ModuleManager moduleManager;
	
	@Inject
	private MetaTranslationRepository translations;

	private ObjectViews validateXML(String xml) {
		try {
			return XMLViews.fromXML(xml);
		} catch (JAXBException e){
			String message = I18n.get("Invalid XML.");
			Throwable ex = e.getLinkedException();
			if (ex != null) {
				message = ex.getMessage().replaceFirst("[^:]+\\:(.*)", "$1");
			}
			throw new IllegalArgumentException(message);
		}
	}

	public void validateAction(ActionRequest request, ActionResponse response) {

		MetaAction meta = request.getContext().asType(MetaAction.class);

		Action action = XMLViews.findAction(meta.getName());
		Map<String, String> data = Maps.newHashMap();

		response.setData(ImmutableList.of(data));

		ObjectViews views;
		try {
			views = validateXML(meta.getXml());
		} catch (Exception e){
			data.put("error", e.getMessage());
			return;
		}

		Action current = views.getActions().get(0);
		if (action != null && !action.getName().equals(current.getName())) {
			data.put("error", I18n.get("Action name can't be changed."));
			return;
		}
	}

	public void validateView(ActionRequest request, ActionResponse response) {
		MetaView meta = request.getContext().asType(MetaView.class);
		Map<String, String> data = Maps.newHashMap();

		try {
			validateXML(meta.getXml());
		} catch (Exception e){
			data.put("error", e.getMessage());
		}

		response.setData(ImmutableList.of(data));
	}

	public void clearCache(ActionRequest request, ActionResponse response) {
		MetaStore.clear();
	}

	/**
	 * Open ModelEntity of the relationship.
	 *
	 * @param request
	 * @param response
	 */
	public void openModel(ActionRequest request, ActionResponse response) {

		MetaField metaField = request.getContext().asType(MetaField.class);

		String domain = String.format("self.packageName = '%s' AND self.name = '%s'", metaField.getPackageName(), metaField.getTypeName());
		response.setView(ActionView
			.define(metaField.getTypeName())
			.model(MetaModel.class.getName())
			.domain(domain)
			.map());
		response.setCanClose(true);
	}

	public void restoreAll(ActionRequest request, ActionResponse response) {
		try {
			MetaStore.clear();
			I18nBundle.invalidate();
			moduleManager.restoreMeta();
			response.setNotify(I18n.get("All views have been restored.") + "<br>" +
					I18n.get("Please refresh your browser to see updated views."));
		} catch (Exception e){
			response.setException(e);
		}
	}
	
	private static final String DEFAULT_EXPORT_DIR = "{java.io.tmpdir}/axelor/data-export";
	private static final String EXPORT_DIR = AppSettings.get().getPath("data.export.dir", DEFAULT_EXPORT_DIR);
	
	private void exportI18n(String module, URL file) throws IOException {

		String name = Paths.get(file.getFile()).getFileName().toString();
		if (!name.startsWith("messages_")) {
			return;
		}
		
		Path path = Paths.get(EXPORT_DIR, "i18n");
		String lang = name.substring(9, name.length() - 4);
		Path target = path.resolve(Paths.get(module, "src/main/resources/i18n", name));

		List<String[]> items = new ArrayList<>();
		CSVReader reader = new CSVReader(new InputStreamReader(file.openStream()));
		try {
			String[] header = reader.readNext();
			String[] values = null;
			while ((values = reader.readNext()) != null) {
				if (header.length != values.length) {
					continue;
				}
				
				final Map<String, String> map = new HashMap<>();
				for (int i = 0; i < header.length; i++) {
					map.put(header[i], values[i]);
				}
				
				String key = map.get("key");
				String value = map.get("value");
				
				if (StringUtils.isBlank(key)) {
					continue;
				}
				
				MetaTranslation tr = translations.findByKey(key, lang);
				if (tr != null) {
					value = tr.getMessage();
				}
				String[] row = {
					key, value, map.get("comment"), map.get("context")
				};
				items.add(row);
			}
		} finally {
			reader.close();
		}
		
		Files.createParentDirs(target.toFile());

		CSVWriter writer = new CSVWriter(new FileWriter(target.toFile()));
		try {
			writer.writeNext(new String[]{"key", "message", "comment", "context"});
			writer.writeAll(items);
		} finally {
			writer.close();
		}
	}

	public void exportI18n(ActionRequest request, ActionResponse response) {
		for (String module : ModuleManager.getResolution()) {
			for (URL file : MetaScanner.findAll(module, "i18n", "(.*?)\\.csv")) {
				try {
					exportI18n(module, file);
				} catch (IOException e) {
					throw Throwables.propagate(e);
				}
			}
		}
		response.setFlash(I18n.get("Export complete."));
	}
}
