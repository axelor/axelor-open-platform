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

import com.axelor.common.ObjectUtils;
import com.axelor.event.Observes;
import com.axelor.events.FeatureChanged;
import com.axelor.events.ModuleChanged;
import com.axelor.events.PostRequest;
import com.axelor.events.qualifiers.EntityType;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.axelor.rpc.Request;
import com.axelor.rpc.Response;
import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Named;

@Singleton
public class ViewObserver {

  private final MetaViewRepository metaViewRepo;

  private final XMLViews.FinalXmlGenerator finalXmlGenerator;

  @Inject
  ViewObserver(MetaViewRepository metaViewRepo, XMLViews.FinalXmlGenerator finalXmlGenerator) {
    this.metaViewRepo = metaViewRepo;
    this.finalXmlGenerator = finalXmlGenerator;
  }

  void onModuleChanged(@Observes ModuleChanged event) {
    finalXmlGenerator.parallelGenerate(metaViewRepo.findByDependentModule(event.getModuleName()));
  }

  void onFeatureChanged(@Observes FeatureChanged event) {
    finalXmlGenerator.parallelGenerate(metaViewRepo.findByDependentFeature(event.getFeatureName()));
  }

  void onPostSave(@Observes @Named("save") @EntityType(MetaView.class) PostRequest event) {
    processResponse(
        event.getResponse(),
        values ->
            Optional.ofNullable((String) values.get("name"))
                .map(metaViewRepo::findByName)
                .ifPresent(finalXmlGenerator::generate));
  }

  private void processRequest(Request request, Consumer<Map<String, Object>> consumer) {
    final Collection<Object> records = getRecords(request.getRecords(), request.getData());
    processRecords(records, consumer);
  }

  private void processResponse(Response response, Consumer<Map<String, Object>> consumer) {
    final Collection<Object> records = getRecords(Collections.emptyList(), response.getData());
    processRecords(records, consumer);
  }

  private Collection<Object> getRecords(Collection<Object> records, Object data) {
    final Collection<Object> allRecords;

    if (ObjectUtils.notEmpty(records)) {
      allRecords = records;
    } else if (data instanceof Collection) {
      @SuppressWarnings("unchecked")
      final Collection<Object> dataAsCollection = (Collection<Object>) data;
      allRecords = dataAsCollection;
    } else if (data != null) {
      allRecords = ImmutableList.of(data);
    } else {
      allRecords = Collections.emptyList();
    }

    return allRecords;
  }

  private void processRecords(Collection<Object> records, Consumer<Map<String, Object>> consumer) {
    records
        .parallelStream()
        .filter(record -> record instanceof Map)
        .forEach(
            record -> {
              @SuppressWarnings("unchecked")
              final Map<String, Object> values = (Map<String, Object>) record;
              consumer.accept(values);
            });
  }
}
