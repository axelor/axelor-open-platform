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
import com.axelor.db.internal.DBHelper;
import com.axelor.db.tenants.TenantAware;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.inject.persist.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class ViewGenerator {

  private static final Logger log = LoggerFactory.getLogger(ViewGenerator.class);

  @Inject private MetaViewRepository metaViewRepo;

  @Transactional
  public boolean generateComputedView(MetaView view) {
    try {
      return _generateComputedView(view);
    } catch (XPathExpressionException
        | ParserConfigurationException
        | SAXException
        | IOException
        | JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  private List<Long> findForCompute(Collection<String> names, boolean update) {
    final boolean namesEmpty = ObjectUtils.isEmpty(names);
    return JPA.em()
        .createQuery(
            "SELECT self.id FROM MetaView self LEFT JOIN self.groups viewGroup WHERE "
                + "((self.name IN :names OR :namesEmpty = TRUE) "
                + "AND (:update = TRUE OR NOT EXISTS ("
                + "SELECT computedView FROM MetaView computedView "
                + "WHERE computedView.name = self.name AND computedView.computed = TRUE))) "
                + "AND COALESCE(self.extension, FALSE) = FALSE "
                + "AND COALESCE(self.computed, FALSE) = FALSE "
                + "AND (self.name, self.priority, COALESCE(viewGroup.id, 0)) "
                + "IN (SELECT other.name, MAX(other.priority), COALESCE(otherGroup.id, 0) FROM MetaView other "
                + "LEFT JOIN other.groups otherGroup "
                + "WHERE COALESCE(other.extension, FALSE) = FALSE AND COALESCE(other.computed, FALSE) = FALSE "
                + "GROUP BY other.name, otherGroup) "
                + "AND EXISTS (SELECT extensionView FROM MetaView extensionView "
                + "WHERE extensionView.name = self.name AND extensionView.extension = TRUE) "
                + "GROUP BY self "
                + "ORDER BY self.id",
            Long.class)
        .setParameter("update", update)
        .setParameter("names", namesEmpty ? ImmutableSet.of("") : names)
        .setParameter("namesEmpty", namesEmpty)
        .getResultList();
  }

  private boolean _generateComputedView(MetaView view)
      throws ParserConfigurationException, SAXException, IOException, XPathExpressionException,
          JAXBException {

    final MetaView originalView = getOriginalView(view);
    final List<MetaView> extensionViews = findExtensionMetaViewsByModuleOrder(originalView);

    originalView.setDependentModules(null);
    originalView.setDependentFeatures(null);

    ComputedViewProcessor processor = new ComputedViewProcessor(originalView, extensionViews);
    MetaView computedView = processor.compute();
    if (computedView == null) {
      return false;
    }

    view.setDependentFeatures(processor.getDependentFeatures());
    view.setDependentModules(processor.getDependentModules());

    metaViewRepo.save(computedView);
    metaViewRepo.save(view);

    return true;
  }

  private List<MetaView> findExtensionMetaViewsByModuleOrder(MetaView view) {
    final List<MetaView> views = findExtensionMetaViews(view);
    final Map<String, List<MetaView>> viewsByModuleName =
        views.parallelStream()
            .collect(Collectors.groupingBy(v -> Optional.ofNullable(v.getModule()).orElse("")));
    final List<MetaView> result = new ArrayList<>(views.size());

    // Add views by module resolution order.
    for (final String moduleName : ModuleManager.getResolution()) {
      result.addAll(viewsByModuleName.getOrDefault(moduleName, Collections.emptyList()));
      viewsByModuleName.remove(moduleName);
    }

    // Add remaining views not found in module resolution.
    for (final List<MetaView> metaViews : viewsByModuleName.values()) {
      result.addAll(metaViews);
    }

    return result;
  }

  private List<MetaView> findExtensionMetaViews(MetaView view) {
    final List<String> select = new ArrayList<>();

    select.add("self.extension = true");
    select.add("self.name = :name");
    select.add("self.model = :model");
    select.add("self.type = :type");

    return metaViewRepo
        .all()
        .filter(Joiner.on(" AND ").join(select))
        .bind("name", view.getName())
        .bind("model", view.getModel())
        .bind("type", view.getType())
        .cacheable()
        .order("-priority")
        .order("id")
        .fetchStream()
        .filter(extView -> Objects.equals(extView.getGroups(), view.getGroups()))
        .collect(Collectors.toList());
  }

  private MetaView getOriginalView(MetaView view) {
    if (Boolean.TRUE.equals(view.getComputed())) {
      log.warn("View is computed: {}", view.getName());
      return Optional.ofNullable(metaViewRepo.findByNameAndComputed(view.getName(), false))
          .orElseThrow(NoSuchElementException::new);
    }

    return view;
  }

  public long process(Collection<String> names, boolean update) {
    final long count = process(findForCompute(names, update));

    if (count == 0L && ObjectUtils.notEmpty(names)) {
      metaViewRepo
          .all()
          .filter("self.name IN :names AND self.computed = TRUE")
          .bind("names", names)
          .remove();
    }

    return count;
  }

  private long process(List<Long> viewsIds) {
    if (ObjectUtils.isEmpty(viewsIds)) {
      return 0L;
    }

    final int numWorkers = findBestNumberOfWorkers(viewsIds.size());
    // create list executed by each worker
    final List<List<Long>> subLists = splitList(viewsIds, viewsIds.size() / numWorkers + 1);
    final Long[] counts = new Long[numWorkers];
    final List<Future<?>> futures = new ArrayList<>(numWorkers);
    final ExecutorService executor = Executors.newFixedThreadPool(numWorkers);

    try {
      IntStream.range(0, numWorkers)
          .forEach(
              index ->
                  futures.add(
                      executor.submit(
                          new TenantAware(() -> counts[index] = generate(subLists.get(index))))));
    } finally {
      executor.shutdown();
    }

    futures.forEach(
        future -> {
          try {
            future.get();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
          }
        });
    JPA.clear();

    return Arrays.stream(counts).mapToLong(Long::longValue).sum();
  }

  static final int CHUNK_SIZE = 20;

  private int findBestNumberOfWorkers(int numberOfItemsToProcess) {
    final int max = DBHelper.getMaxWorkers();
    final int nbr = Math.max(numberOfItemsToProcess / CHUNK_SIZE, 1);
    return Math.min(nbr, max);
  }

  private List<List<Long>> splitList(List<Long> initialList, int chunkSize) {
    final AtomicInteger counter = new AtomicInteger();
    return new ArrayList<>(
        initialList.stream()
            .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
            .values());
  }

  private long generate(List<Long> viewsIds) {
    long count = 0;

    // Create consecutive sublists of a list,
    // each of the same size (the final list may be smaller)
    for (List<Long> items : splitList(viewsIds, CHUNK_SIZE)) {
      count += generateComputedView(JPA.findByIds(MetaView.class, items));
      JPA.clear();
    }

    return count;
  }

  private long generateComputedView(List<MetaView> viewsIds) {
    return viewsIds.stream()
        .map(view -> generateComputedView(view) ? 1L : 0L)
        .mapToLong(Long::longValue)
        .sum();
  }
}
