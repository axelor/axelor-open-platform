/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.axelor.db.JPA;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaField;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.MetaTranslation;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.loader.I18nLoader;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.service.MetaExportTranslation;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class MetaController {

	@Inject
	private I18nLoader i18nLoader;

	@Inject
	private MetaExportTranslation export;

	@Inject
	private ModuleManager moduleManager;

	private ObjectViews validateXML(String xml) {
		ObjectViews views;
		try {
			views = XMLViews.fromXML(xml);
		} catch (JAXBException e){
			String message = JPA.translate("Invalid XML.");
			Throwable ex = e.getLinkedException();
			if (ex != null) {
				message = ex.getMessage().replaceFirst("[^:]+\\:(.*)", "$1");
			}
			throw new IllegalArgumentException(message);
		}
		return views;
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
			data.put("error", JPA.translate("Action name can't be changed."));
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
		Map<String, Object> view = new HashMap<String, Object>();
		view.put("title", metaField.getTypeName());
		view.put("resource", MetaModel.class.getName());
		view.put("domain", domain);
		response.setView(view);
   }

	public void restoreTranslations(ActionRequest request, ActionResponse response) {
		Map<String, String> data = Maps.newHashMap();
		String importPath = (String) request.getContext().get("importPath");
		String importType = (String) request.getContext().get("importType");

		if(Strings.isNullOrEmpty(importType)) {
			data.put("error", JPA.translate("Please select an import type first."));
			response.setData(ImmutableList.of(data));
			return;
		}
		if(importType.equals("2") && Strings.isNullOrEmpty(importPath)) {
			data.put("error", JPA.translate("Please enter your import path fisrt."));
			response.setData(ImmutableList.of(data));
			return;
		}

		i18nLoader.load(importPath);

		response.setFlash(JPA.translate("Import done."));
		response.setHidden("exportGroup", true);

		MetaTranslation view = MetaTranslation.all().fetchOne();
		response.setValues(view);
		response.setReload(true);
	}

	public void exportTranslations(ActionRequest request, ActionResponse response) {

		String exportPath = (String) request.getContext().get("exportPath");
		String exportLanguage = (String) request.getContext().get("exportLanguage");
		Map<String, String> data = Maps.newHashMap();

		if(Strings.isNullOrEmpty(exportLanguage)) {
			data.put("error", JPA.translate("Please enter your export language fisrt."));
			response.setData(ImmutableList.of(data));
			return;
		}
		if(Strings.isNullOrEmpty(exportPath)) {
			data.put("error", JPA.translate("Please enter your export path fisrt."));
			response.setData(ImmutableList.of(data));
			return;
		}

		try {

			export.exportTranslations(exportPath, exportLanguage);

			response.setFlash(JPA.translate("Export done."));
			response.setHidden("exportGroup", true);

			data.put("exportPath", null);
			data.put("exportLanguage", null);
			response.setValues(data);

		}
		catch(Exception e) {
			data.put("error", e.getMessage());
			response.setData(ImmutableList.of(data));
		}
	}

	public void restoreAll(ActionRequest request, ActionResponse response) {
		Map<String, String> data = Maps.newHashMap();

		try {
			MetaStore.clear();
			moduleManager.update(false);
		} catch (Exception e){
			data.put("error", e.getMessage());
		}

		response.setData(ImmutableList.of(data));
	}

	public void restoreSingle(ActionRequest request, ActionResponse response) {
		//TODO
	}
}
