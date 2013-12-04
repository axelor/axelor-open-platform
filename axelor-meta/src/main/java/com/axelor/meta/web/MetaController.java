/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.meta.web;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.axelor.db.JPA;
import com.axelor.meta.MetaLoader;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaField;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.MetaTranslation;
import com.axelor.meta.db.MetaUser;
import com.axelor.meta.db.MetaView;
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
	private MetaLoader loader;

	@Inject
	private MetaExportTranslation export;

	private ObjectViews validateXML(String xml) {
		ObjectViews views;
		try {
			views = loader.fromXML(xml);
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

		Action action = loader.findAction(meta.getName());
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

	public void restoreAll(ActionRequest request, ActionResponse response) {

		MetaStore.clear();
		final Map<Long, String> userActions = Maps.newHashMap();

		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {

				// backup user actions
				for(MetaUser user : MetaUser.all().fetch()) {
					if (user.getAction() != null) {
						userActions.put(user.getId(), user.getAction().getName());
					}
				}

				JPA.clear();
				JPA.em().createNativeQuery("UPDATE meta_user SET action = NULL").executeUpdate();
				JPA.em().createNativeQuery("DELETE FROM meta_menu_groups").executeUpdate();
				JPA.em().createNativeQuery("DELETE FROM meta_view_groups").executeUpdate();
				JPA.em().createNativeQuery("DELETE FROM meta_menu").executeUpdate();
				JPA.em().createNativeQuery("DELETE FROM meta_action_menu").executeUpdate();
				JPA.em().createNativeQuery("DELETE FROM meta_action").executeUpdate();
				JPA.em().createNativeQuery("DELETE FROM meta_view").executeUpdate();
				JPA.em().createNativeQuery("DELETE FROM meta_select_item").executeUpdate();
				JPA.em().createNativeQuery("DELETE FROM meta_select").executeUpdate();
			}
		});

		loader.load(null);

		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {

				// restore use actions
				for(Long id : userActions.keySet()) {
					MetaUser user = MetaUser.find(id);
					user.setAction(MetaAction.findByName(userActions.get(id)));
				}
			}
		});

		MetaView view = MetaView.all().fetchOne();
		response.setValues(view);
		response.setReload(true);
	}

	public void restoreSingle(ActionRequest request, ActionResponse response) {
		final MetaView meta = request.getContext().asType(MetaView.class);
		Map<String, String> data = Maps.newHashMap();

		if(Strings.isNullOrEmpty(meta.getName())) {
			data.put("error", JPA.translate("Please specify the view name."));
			response.setData(ImmutableList.of(data));
			return;
		}
		else if(Strings.isNullOrEmpty(meta.getModule())) {
			data.put("error", JPA.translate("Please specify the module name."));
			response.setData(ImmutableList.of(data));
			return;
		}

		MetaStore.clear();
		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {
				JPA.clear();
				JPA.em().createNativeQuery("DELETE FROM meta_view where id = ?1").setParameter(1, meta.getId()).executeUpdate();
			}
		});

		Boolean imported = loader.loadSingleViews(meta.getName(), meta.getModule());
		if(!imported) {
			JPA.runInTransaction(new Runnable() {

				@Override
				public void run() {
					meta.setId(null);
					meta.setVersion(null);
					meta.save();
				}
			});
		}

		MetaView view = MetaView.all().filter("self.name = ?1 AND self.module = ?2", meta.getName(), meta.getModule()).fetchOne();
		response.setValues(view);
		response.setReload(true);
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

		String importPath = (String) request.getContext().get("importPath");
		String importType = (String) request.getContext().get("importType");

		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {
				JPA.clear();
				JPA.em().createNativeQuery("DELETE FROM meta_translation").executeUpdate();
			}
		});

		try {
			if(Strings.isNullOrEmpty(importType)) {
				throw new Exception(JPA.translate("Please select an import type first."));
			}
			if(importType.equals("2") && Strings.isNullOrEmpty(importPath)) {
				throw new Exception(JPA.translate("Please enter your import path fisrt."));
			}
			loader.loadTranslations(importPath);

			response.setFlash(JPA.translate("Import done."));
			response.setHidden("exportGroup", true);

			MetaTranslation view = MetaTranslation.all().fetchOne();
			response.setValues(view);
		} catch(Exception e) {
			response.setFlash(e.getLocalizedMessage());
		}
	}

	public void exportTranslations(ActionRequest request, ActionResponse response) {

		String exportPath = (String) request.getContext().get("exportPath");
		String exportLanguage = (String) request.getContext().get("exportLanguage");
		Map<String, String> data = Maps.newHashMap();

		try {
			if(Strings.isNullOrEmpty(exportLanguage)) {
				throw new Exception(JPA.translate("Please enter your export language fisrt."));
			}
			if(Strings.isNullOrEmpty(exportPath)) {
				throw new Exception(JPA.translate("Please enter your export path fisrt."));
			}
			export.exportTranslations(exportPath, exportLanguage);

			response.setFlash(JPA.translate("Export done."));
			response.setHidden("exportGroup", true);

			data.put("exportPath", null);
			data.put("exportLanguage", null);
			response.setValues(data);
		} catch(Exception e) {
			response.setFlash(e.getLocalizedMessage());
		}
	}
}
