/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.team.db.repo;

import com.axelor.auth.AuthUtils;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.repo.MailFollowerRepository;
import com.axelor.team.db.Team;
import jakarta.persistence.TypedQuery;
import java.util.Map;

public class TeamRepository extends JpaRepository<Team> {

  public TeamRepository() {
    super(Team.class);
  }

  public Team findByName(String name) {
    return all().filter("self.name = :name").bind("name", name).fetchOne();
  }

  @Override
  public Team save(Team entity) {
    final Team team = super.save(entity);
    final MailFollowerRepository followers = Beans.get(MailFollowerRepository.class);

    if (team.getMembers() != null) {
      team.getMembers().forEach(user -> followers.follow(team, user));
    } else {
      followers.findAll(entity).forEach(followers::unfollow);
    }

    final TypedQuery<MailFollower> query =
        JPA.em()
            .createQuery(
                """
                SELECT f FROM MailFollower f \
                LEFT JOIN f.user u \
                WHERE f.relatedModel = :model \
                	AND f.relatedId = :id \
                	AND u.id NOT IN \
                		(SELECT x.id FROM Team t LEFT JOIN t.members x WHERE t.id = :id)""",
                MailFollower.class);

    query.setParameter("model", Team.class.getName());
    query.setParameter("id", team.getId());
    query.getResultList().forEach(f -> followers.unfollow(team, f.getUser()));

    return team;
  }

  @Override
  public void remove(Team entity) {
    final MailFollowerRepository followers = Beans.get(MailFollowerRepository.class);
    followers.findAll(entity).forEach(followers::unfollow);
    super.remove(entity);
  }

  @Override
  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
    if (json == null || json.get("id") == null) {
      return json;
    }

    final Team entity = find((Long) json.get("id"));
    if (entity == null) {
      return json;
    }

    final MailFollowerRepository followers = Beans.get(MailFollowerRepository.class);
    final MailFollower follower = followers.findOne(entity, AuthUtils.getUser());

    json.put("_following", follower != null && Boolean.FALSE.equals(follower.getArchived()));
    json.put("_image", entity.getImage() != null);

    return json;
  }
}
