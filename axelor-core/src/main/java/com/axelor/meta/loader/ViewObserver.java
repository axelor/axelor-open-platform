/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
import com.axelor.event.Observes;
import com.axelor.events.FeatureChanged;
import com.axelor.events.ModuleChanged;
import com.axelor.events.PostRequest;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.qualifiers.EntityType;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.axelor.rpc.RequestUtils;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

@Singleton
public class ViewObserver {

  private final MetaViewRepository metaViewRepo;

  private final XMLViews.FinalViewGenerator finalViewGenerator;

  private final Set<String> toRegenerate = new HashSet<>();

  @Inject
  ViewObserver(MetaViewRepository metaViewRepo, XMLViews.FinalViewGenerator finalViewGenerator) {
    this.metaViewRepo = metaViewRepo;
    this.finalViewGenerator = finalViewGenerator;
  }

  void onModuleChanged(@Observes ModuleChanged event) {
    finalViewGenerator.generate(metaViewRepo.findByDependentModule(event.getModuleName()));
  }

  void onFeatureChanged(@Observes FeatureChanged event) {
    finalViewGenerator.generate(metaViewRepo.findByDependentFeature(event.getFeatureName()));
  }

  void onPostSave(
      @Observes @Named(RequestEvent.SAVE) @EntityType(MetaView.class) PostRequest event) {
    RequestUtils.processResponse(
        event.getResponse(),
        values ->
            Optional.ofNullable((String) values.get("name"))
                .map(name -> metaViewRepo.findByNameAndComputed(name, false))
                .filter(view -> !(Boolean) values.getOrDefault("computed", false))
                .ifPresent(finalViewGenerator::generate));
  }

  void onPreRemove(
      @Observes @Named(RequestEvent.REMOVE) @EntityType(MetaView.class) PreRequest event) {
    RequestUtils.processRequest(
        event.getRequest(),
        values -> {
          final MetaView view = JPA.edit(MetaView.class, values);
          final String name = view.getName();
          toRegenerate.add(name);
        });
  }

  void onPostRemove(
      @Observes @Named(RequestEvent.REMOVE) @EntityType(MetaView.class) PostRequest event) {
    toRegenerate.stream()
        .map(name -> metaViewRepo.findByNameAndComputed(name, false))
        .filter(Objects::nonNull)
        .forEach(finalViewGenerator::generate);
    toRegenerate.clear();
  }
}
