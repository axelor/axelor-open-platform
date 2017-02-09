package com.axelor.meta.db.repo;

import javax.persistence.EntityManager;

import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaJsonModel;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaView;
import com.google.inject.persist.Transactional;

public class MetaJsonModelRepository extends AbstractMetaJsonModelRepository {

	private void onSave(MetaJsonModel jsonModel) {
		if (jsonModel.getFields() == null) {
			return;
		}

		MetaMenu menu = jsonModel.getMenu();
		if (menu == null) {
			menu = new MetaMenu();
		}

		menu.setName("menu-json-model-" + jsonModel.getName());
		menu.setTitle(jsonModel.getTitle());

		MetaAction action = jsonModel.getAction();
		if (action == null) {
			action = new MetaAction();
			action.setType("action-view");
			action.setModel(MetaJsonRecord.class.getName());
		}
		action.setName("all.json." + jsonModel.getName());

		MetaView gridView = jsonModel.getGridView();
		if (gridView == null) {
			gridView = new MetaView();
			gridView.setType("grid");
			gridView.setModel(action.getModel());
		}
		gridView.setName("custom-model-" + jsonModel.getName() + "-grid");
		gridView.setTitle(jsonModel.getTitle());
		gridView.setXml(new StringBuilder()
				.append("<grid")
				.append(" name=").append('"').append(gridView.getName()).append('"')
				.append(" title=").append('"').append(gridView.getTitle()).append('"')
				.append(" model=").append('"').append(gridView.getModel()).append('"')
				.append(">\n")
				.append("  <field name=\"attrs\" x-json-model=\"" + jsonModel.getName() + "\"/>\n")
				.append("</grid>\n")
				.toString());

		MetaView formView = jsonModel.getFormView();
		if (formView == null) {
			formView = new MetaView();
			formView.setType("form");
			formView.setModel(action.getModel());
		}
		formView.setName("custom-model-" + jsonModel.getName() + "-form");
		formView.setTitle(jsonModel.getTitle());
		formView.setXml(new StringBuilder()
				.append("<form")
				.append(" name=").append('"').append(gridView.getName()).append('"')
				.append(" title=").append('"').append(gridView.getTitle()).append('"')
				.append(" model=").append('"').append(gridView.getModel()).append('"')
				.append(" onNew=").append('"').append("action-json-record-defaults").append('"')
				.append(">\n")
				.append("  <panel title=\"Overview\" itemSpan=\"12\">\n")
				.append("    <field name=\"attrs\" x-json-model=\"" + jsonModel.getName() + "\"/>\n")
				.append("  </panel>\n")
				.append("</form>\n")
				.toString());

		action.setXml(new StringBuilder()
				.append("<action-view")
				.append(" name=").append('"').append(action.getName()).append('"')
				.append(" title=").append('"').append(menu.getTitle()).append('"')
				.append(" model=").append('"').append(action.getModel()).append('"')
				.append(">\n")
				.append("  <view type=\"grid\" name=\"").append(gridView.getName()).append("\" />\n")
				.append("  <view type=\"form\" name=\"").append(formView.getName()).append("\" />\n")
				.append("  <domain>self.jsonModel = :jsonModel</domain>\n")
				.append("  <context name=\"jsonModel\" expr=\""+ jsonModel.getName() +"\" />\n")
				.append("</action-view>\n").toString());
		menu.setAction(action);

		jsonModel.setMenu(menu);
		jsonModel.setAction(action);
		jsonModel.setGridView(gridView);
		jsonModel.setFormView(formView);
	}

	@Override
	@Transactional
	public MetaJsonModel save(MetaJsonModel entity) {
		this.onSave(entity);
		return super.save(entity);
	}

	@Override
	@Transactional
	public void remove(MetaJsonModel entity) {
		final EntityManager em = JPA.em();
		final Model[] related = {
			entity.getGridView(),
			entity.getFormView(),
			entity.getMenu(),
			entity.getAction(),
		};

		for (Model item : related) {
			if (item != null) {
				em.remove(item);
			}
		}

		super.remove(entity);
		JPA.all(MetaJsonRecord.class).filter("self.jsonModel = ?", entity.getName()).remove();
	}
}
