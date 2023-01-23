/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.meta.loader;

import com.axelor.common.ObjectUtils;
import com.axelor.db.JPA;
import com.axelor.event.Observes;
import com.axelor.events.FeatureChanged;
import com.axelor.events.PostRequest;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.qualifiers.EntityType;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.axelor.rpc.RequestUtils;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

@Singleton
public class ViewObserver {

  private final MetaViewRepository metaViewRepo;

  private final ViewGenerator viewGenerator;

  private final Set<String> toRegenerate = new HashSet<>();

  @Inject
  ViewObserver(MetaViewRepository metaViewRepo, ViewGenerator viewGenerator) {
    this.metaViewRepo = metaViewRepo;
    this.viewGenerator = viewGenerator;
  }

  void onFeatureChanged(@Observes FeatureChanged event) {
    List<String> names =
        metaViewRepo
            .findByDependentFeature(event.getFeatureName())
            .select("name")
            .fetch(0, 0)
            .stream()
            .map(it -> it.get("name").toString())
            .collect(Collectors.toList());
    if (ObjectUtils.isEmpty(names)) {
      return;
    }
    viewGenerator.process(names, true);
  }

  void onPostSave(
      @Observes @Named(RequestEvent.SAVE) @EntityType(MetaView.class) PostRequest event) {
    RequestUtils.processResponse(
        event.getResponse(),
        values -> {
          final MetaView view = JPA.edit(MetaView.class, values);
          if (!Boolean.TRUE.equals(view.getComputed())) {
            viewGenerator.process(Collections.singletonList(view.getName()), true);
          }
        });
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
    try {
      viewGenerator.process(toRegenerate, true);
    } finally {
      toRegenerate.clear();
    }
  }
}
