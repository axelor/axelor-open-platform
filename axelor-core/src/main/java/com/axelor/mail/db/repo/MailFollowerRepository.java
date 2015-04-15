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
package com.axelor.mail.db.repo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import com.axelor.mail.db.MailFollower;
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
			all.add(Resource.toMapCompact(follower.getUser()));
		}

		return all;
	}

	@Transactional
	public void follow(Model entity, User user) {

		if (isFollowing(entity, user)) {
			return;
		}

		final MailFollower follower = new MailFollower();
		follower.setRelatedId(entity.getId());
		follower.setRelatedModel(entity.getClass().getName());
		follower.setUser(user);

		save(follower);
	}

	@Transactional
	public void unfollow(Model entity, User user) {

		if (!isFollowing(entity, user)) {
			return;
		}

		MailFollower follower = findOne(entity, user);

		if (follower != null) {
			remove(follower);
		}
	}

	public boolean isFollowing(Model entity, User user) {
		MailFollower found = findOne(entity, user);
		return found != null;
	}
}
