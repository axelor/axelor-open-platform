/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.meta.loader;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.event.Observes;
import com.axelor.events.FeatureChanged;
import com.axelor.events.ModuleChanged;
import com.axelor.events.PostRequest;
import com.axelor.events.qualifiers.EntityType;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

@Singleton
public class ViewObserver {

  private final MetaViewRepository metaViewRepo;

  private static final int FETCH_LIMT = 2_000;

  @Inject
  public ViewObserver(MetaViewRepository metaViewRepo) {
    this.metaViewRepo = metaViewRepo;
  }

  void onModuleChanged(@Observes ModuleChanged event) {
    clearFinalXml(metaViewRepo.findByDependentModule(event.getModuleName()));
  }

  void onFeatureChanged(@Observes FeatureChanged event) {
    clearFinalXml(metaViewRepo.findByDependentFeature(event.getFeatureName()));
  }

  void onViewChanged(@Observes @Named("save") @EntityType(MetaView.class) PostRequest event) {
    final Map<String, Object> data = event.getRequest().getData();
    final MetaView view = JPA.edit(MetaView.class, data);
    clearFinalXml(metaViewRepo.findAllByName(view.getName()));
  }

  private void clearFinalXml(Query<MetaView> query) {
    int offset = 0;
    List<MetaView> views;

    while (!(views = query.fetch(FETCH_LIMT, offset)).isEmpty()) {
      clearFinalXml(views);
      offset += views.size();
    }
  }

  @Transactional
  void clearFinalXml(List<MetaView> views) {
    views.forEach(view -> view.setFinalXml(null));
  }
}
