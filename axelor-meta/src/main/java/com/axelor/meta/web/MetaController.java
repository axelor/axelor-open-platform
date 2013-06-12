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
import com.axelor.meta.service.MetaService;
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
			String message = "Invalid XML.";
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
			data.put("error", "Action name can't be changed.");
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
		
		JPA.runInTransaction(new Runnable() {
			
			@Override
			public void run() {
				JPA.clear();
				JPA.em().createNativeQuery("DELETE FROM meta_translation").executeUpdate();
			}
		});
		
		loader.loadTranslations();
		
		MetaTranslation view = MetaTranslation.all().fetchOne();
		response.setValues(view);
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
			export.exportTranslations(exportPath,exportLanguage);
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
