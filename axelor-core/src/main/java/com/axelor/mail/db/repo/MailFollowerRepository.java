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
package com.axelor.mail.db.repo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.MailGroup;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.repo.MetaActionRepository;
import com.axelor.meta.db.repo.MetaMenuRepository;
import com.axelor.rpc.Resource;
import com.google.inject.persist.Transactional;

public class MailFollowerRepository extends JpaRepository<MailFollower> {

	public MailFollowerRepository() {
		super(MailFollower.class);
	}

	public List<MailFollower> findAll(Model entity) {
		return findAll(entity, -1);
	}

	public List<MailFollower> findAll(Model entity, int limit) {
		return all().filter("self.relatedModel = ? AND self.relatedId = ?",
				entity.getClass().getName(), entity.getId()).fetch(limit);
	}

	public MailFollower findOne(Model entity, User user) {
		MailFollower follower = all()
				.filter("self.relatedId = ? AND self.relatedModel = ? AND self.user.id = ?",
						entity.getId(), entity.getClass().getName(),
						user.getId()).fetchOne();
		return follower;
	}

	public List<Map<String, Object>> findFollowers(Model entity) {

		if (entity == null || entity.getId() == null) {
			return null;
		}

		final List<MailFollower> users = findAll(entity);

		if (users == null || users.isEmpty()) {
			return null;
		}

		final List<Map<String, Object>> all = new ArrayList<>();
		for (MailFollower follower : users) {
			if (follower.getArchived() == Boolean.TRUE) {
				all.add(Resource.toMapCompact(follower.getUser()));
			}
		}

		return all;
	}

	private void createOrDeleteMenu(MailGroup entity,  boolean delete) {
		final MetaActionRepository actionRepo = Beans.get(MetaActionRepository.class);
		final MetaMenuRepository menuRepo = Beans.get(MetaMenuRepository.class);
		final MetaMenu parent = menuRepo.findByName("menu-mail-groups");

		if (parent == null) {
			return;
		}

		final String name = "menu-mail-groups-" + entity.getId();
		final String actionName = "mail.groups." + entity.getId();

		MetaMenu menu = menuRepo.all().filter("self.name = ? AND self.user = ?", name, AuthUtils.getUser()).fetchOne();
		MetaAction action = actionRepo.findByName(actionName);

		if (delete) {
			if (menu != null) {
				menuRepo.remove(menu);
			}
			if (action != null) {
				// check if action is referenced by other users
				if (menuRepo.all().filter("self.action.name = ?", actionName).count() == 0) {
					actionRepo.remove(action);
				}
			}
			return;
		}

		if (action == null) {
			action = new MetaAction(actionName);
			action.setType("action-view");
			action.setModel(MailGroup.class.getName());
			action.setXml(""
					+ "<action-view title='"+ entity.getName() + "' name='" + actionName + "' model='"+ MailGroup.class.getName() +"'>\n"
					+ "  <view type='form'/>\n"
					+ "  <view-param name='ui-template:form' value='mail-group-form'/>\n"
					+ "  <context name='_showRecord' expr='eval: "+ entity.getId() +"'/>\n"
					+ "</action-view>");
		}

		if (menu == null) {
			menu = new MetaMenu();
			menu.setName(name);
			menu.setTitle(entity.getName());
			menu.setIcon("fa-group");
			menu.setOrder(-50);
			menu.setParent(parent);
			menu.setAction(action);
			menu.setUser(AuthUtils.getUser());
		}

		menuRepo.save(menu);
	}

	@Transactional
	public void follow(Model entity, User user) {

		MailFollower follower = findOne(entity, user);
		if (follower != null && follower.getArchived() == Boolean.TRUE) {
			return;
		}

		if (follower == null) {
			follower = new MailFollower();
		}

		follower.setArchived(true);
		follower.setRelatedId(entity.getId());
		follower.setRelatedModel(entity.getClass().getName());
		follower.setUser(user);

		// create menu
		if (entity instanceof MailGroup) {
			createOrDeleteMenu((MailGroup) entity, false);
		}

		save(follower);
	}

	@Transactional
	public void unfollow(Model entity, User user) {

		if (!isFollowing(entity, user)) {
			return;
		}

		MailFollower follower = findOne(entity, user);
		if (follower != null) {
			follower.setArchived(false);
			save(follower);
		}

		// remove menu
		if (entity instanceof MailGroup) {
			createOrDeleteMenu((MailGroup) entity, true);
		}
	}

	public boolean isFollowing(Model entity, User user) {
		final MailFollower found = findOne(entity, user);
		return found != null && found.getArchived() == Boolean.TRUE;
	}
}
