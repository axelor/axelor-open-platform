package com.axelor.meta.loader;

import com.axelor.common.ObjectUtils;
import com.axelor.db.JPA;
import com.axelor.db.internal.DBHelper;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.inject.persist.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.TypedQuery;
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

  private TypedQuery<MetaView> findForCompute(Collection<String> names, boolean update) {
    final boolean namesEmpty = ObjectUtils.isEmpty(names);
    return JPA.em()
        .createQuery(
            "SELECT self FROM MetaView self LEFT JOIN self.groups viewGroup WHERE "
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
            MetaView.class)
        .setParameter("update", update)
        .setParameter("names", namesEmpty ? ImmutableSet.of("") : names)
        .setParameter("namesEmpty", namesEmpty);
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

  @Transactional
  public long generate(Collection<String> names, boolean update) {
    TypedQuery<MetaView> query = findForCompute(names, update);
    final long count = generate(query, 0, DBHelper.getJdbcFetchSize());

    if (count == 0L && ObjectUtils.notEmpty(names)) {
      metaViewRepo
          .all()
          .filter("self.name IN :names AND self.computed = TRUE")
          .bind("names", names)
          .remove();
    }

    return count;
  }

  private long generate(TypedQuery<MetaView> query, int startOffset, int increment) {
    List<MetaView> views;
    int offset = startOffset;
    long count = 0;

    query.setMaxResults(DBHelper.getJdbcFetchSize());
    query.setFirstResult(offset);
    while (!(views = query.getResultList()).isEmpty()) {
      count += generateComputedView(views);
      offset += increment;
      query.setFirstResult(offset);
    }

    return count;
  }

  private long generateComputedView(List<MetaView> views) {
    return views.stream()
        .map(view -> generateComputedView(view) ? 1L : 0L)
        .mapToLong(Long::longValue)
        .sum();
  }
}
