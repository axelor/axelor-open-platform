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
package com.axelor.meta.db.repo;

import javax.persistence.EntityManager;

import com.axelor.common.StringUtils;
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

		if (jsonModel.getRoles() != null) {
			jsonModel.getRoles().forEach(menu::addRole);
		}

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

		String onNew = jsonModel.getOnNew();
		String onSave = jsonModel.getOnSave();

		onNew = StringUtils.isBlank(onNew) ?
				"action-json-record-defaults" :
				"action-json-record-defaults," + onNew;

		MetaView formView = jsonModel.getFormView();
		if (formView == null) {
			formView = new MetaView();
			formView.setType("form");
			formView.setModel(action.getModel());
		}
		formView.setName("custom-model-" + jsonModel.getName() + "-form");
		formView.setTitle(jsonModel.getTitle());
		StringBuilder xml = new StringBuilder()
				.append("<form")
				.append(" name=").append('"').append(formView.getName()).append('"')
				.append(" title=").append('"').append(formView.getTitle()).append('"')
				.append(" model=").append('"').append(formView.getModel()).append('"')
				.append(" onNew=").append('"').append(onNew).append('"');
		
		if (!StringUtils.isBlank(onSave)) {
			xml.append(" onSave=").append('"').append(onSave).append('"');
		}

		xml.append(">\n")
				.append("  <panel title=\"Overview\" itemSpan=\"12\">\n")
				.append("    <field name=\"attrs\" x-json-model=\"" + jsonModel.getName() + "\"/>\n")
				.append("  </panel>\n")
				.append("</form>\n");

		formView.setXml(xml.toString());

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
